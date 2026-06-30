package com.llosa.backend.documentos.service;

import com.google.cloud.storage.*;
import com.llosa.backend.comercial.dto.StageDocumentsResponse;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.dto.SubirDocumentoRequest;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.entity.TipoDocumentoConfig;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.repository.TipoDocumentoConfigRepository;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private static final long SIGNED_URL_MINUTES = 15L;

    private final DocumentoRepository documentoRepository;
    private final TipoDocumentoConfigRepository tipoDocumentoConfigRepository;
    private final EntidadResolverService entidadResolver;
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final Storage storage;
    private final String gcsBucketName;

    // ─── Stage / Contrato ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StageDocumentsResponse obtenerDetalleEtapa(EtapaProceso etapaProceso, UUID uuidUsuarioActivo) {
        if (etapaProceso != EtapaProceso.CONTRATO) {
            return new StageDocumentsResponse(null, 0, List.of());
        }

        UsuarioActivo ua = usuarioActivoRepository.findById(uuidUsuarioActivo)
                .orElseThrow(() -> new EntityNotFoundException("UsuarioActivo no encontrado: " + uuidUsuarioActivo));

        List<StageDocumentsResponse.DocumentoItem> unidades = new ArrayList<>();
        BigDecimal areaTotal = BigDecimal.ZERO;

        if (ua.getActivos() != null) {
            for (Activo activo : ua.getActivos()) {
                if (activo != null) {
                    unidades.add(mapActivoToItem(activo));
                    if (activo.getAreaM2() != null) {
                        areaTotal = areaTotal.add(activo.getAreaM2());
                    }
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
        String firmaContrato = ua.getFechaAdquisicion() != null
                ? ua.getFechaAdquisicion().format(formatter)
                : null;

        String resumen = String.format(Locale.US,
                "Contrato firmado: %s | Financiamiento: %s | Área total: %.2f m²",
                firmaContrato != null ? firmaContrato : "Pendiente",
                ua.getTipoFinanciamiento() != null ? ua.getTipoFinanciamiento() : "-",
                areaTotal.doubleValue());

        return new StageDocumentsResponse(resumen, unidades.size(), unidades);
    }

    private StageDocumentsResponse.DocumentoItem mapActivoToItem(Activo activo) {
        TipoActivo tipoEnum = activo.getTipo();

        // 2. Usamos switch moderno como expresión para asignar las variables limpiamente
        String tipo = switch (tipoEnum != null ? tipoEnum : TipoActivo.DEPOSITO) {
            case DEPARTAMENTO -> "DEPARTAMENTO";
            case COCHERA      -> "ESTACIONAMIENTO";
            case DEPOSITO     -> "OTRO"; // Mapea DEPOSITO (o nulos) a "OTRO" como tu código original
        };

        String nombre = switch (tipoEnum != null ? tipoEnum : TipoActivo.DEPOSITO) {
            case DEPARTAMENTO -> "Dpto. " + activo.getNro();
            case COCHERA      -> "Cochera " + activo.getNro();
            case DEPOSITO     -> activo.getNro();
        };

        String icono = switch (tipoEnum != null ? tipoEnum : TipoActivo.DEPOSITO) {
            case DEPARTAMENTO -> "edificio";
            case COCHERA      -> "parking";
            case DEPOSITO     -> "file";
        };

        DecimalFormat df = new DecimalFormat("#,###");
        String aporte = "S/." + df.format(activo.getPrecio());
        String area = String.format(Locale.US, "%.2f m²", activo.getAreaM2());

        return new StageDocumentsResponse.DocumentoItem(
                activo.getId().toString(),
                nombre,
                tipo + " | " + area,
                "active",
                null,
                false,
                null,
                false,
                aporte,
                icono
        );
    }

    // ─── Subir documento (genérico — resuelve entidad automáticamente) ───────────

    @Transactional
    public DocumentoResponse subirDocumento(
            UUID idReferencia,
            MultipartFile file,
            SubirDocumentoRequest request,
            Integer subidoPor
    ) {
        // FIX 0000850: validamos el archivo (extensión, mime, tamaño) ANTES de
        // resolver la entidad. La resolución de entidad implica hasta 9 consultas
        // secuenciales a la base de datos; si el archivo es inválido no tiene
        // sentido pagar ese costo. Esto permite responder con el error de forma
        // inmediata en lugar de "intentar la carga" antes de fallar.
        validarArchivo(file, request.tipoDocumento());

        String entidad = entidadResolver.resolverEntidad(idReferencia);

        // CORRECCIÓN: Llamamos a la versión limpia sin parámetros fantasma
        return subirDocumentoPolimorfico(
                file, request.tipoDocumento(),
                idReferencia.toString(), entidad, subidoPor
        );
    }

    // ─── Subir documento (polimórfico — entidad y referencia explícitas) ─────────

    @Transactional
    public DocumentoResponse subirDocumentoPolimorfico(
            // CORRECCIÓN: Eliminado el UUID usuarioActivoId que rompía el polimorfismo
            MultipartFile file,
            TipoDocumento tipoDocumento,
            String idReferencia,
            String entidadReferencia,
            Integer subidoPor
    ) {
        validarArchivo(file, tipoDocumento);

        String extension = obtenerExtension(file.getOriginalFilename());

        // CORRECCIÓN: La ruta GCS ahora organiza las carpetas dinámicamente según la entidad y su ID real
        String rutaGcs = String.format("proyectos/%s/%s/%s.%s",
                entidadReferencia.toLowerCase(), idReferencia, UUID.randomUUID(), extension);

        try {
            BlobId blobId = BlobId.of(gcsBucketName, rutaGcs);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();
            storage.create(blobInfo, file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("Error al subir el archivo a GCS: " + e.getMessage());
        }

        Documento documento = Documento.builder()
                .rutaGcs(rutaGcs)
                .nombreOriginal(file.getOriginalFilename())
                .idReferencia(idReferencia)
                .entidadReferencia(entidadReferencia)
                .tipoDocumento(tipoDocumento)
                .tipoMime(file.getContentType())
                .accesoRestringido(true)
                .subidoPor(subidoPor)
                .build();

        return DocumentoResponse.fromEntity(documentoRepository.save(documento));
    }

    // ─── Listar documentos ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listar(UUID idReferencia, TipoDocumento tipoDocumento) {
        String entidad = entidadResolver.resolverEntidad(idReferencia);

        List<Documento> documentos = tipoDocumento != null
                ? documentoRepository.findByIdReferenciaAndEntidadReferenciaAndTipoDocumento(
                idReferencia.toString(), entidad, tipoDocumento)
                : documentoRepository.findByIdReferenciaAndEntidadReferencia(
                idReferencia.toString(), entidad);

        return documentos.stream().map(DocumentoResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> obtenerPorReferencia(String entidadReferencia, String idReferencia) {
        return documentoRepository
                .findByIdReferenciaAndEntidadReferencia(idReferencia, entidadReferencia)
                .stream()
                .map(doc -> DocumentoResponse.fromEntity(doc, firmarUrl(doc.getRutaGcs())))
                .toList();
    }

    // ─── Signed URL ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SignedUrlResponse generarSignedUrl(UUID documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado: " + documentoId));

        Instant expiracion = Instant.now().plusSeconds(SIGNED_URL_MINUTES * 60);
        return new SignedUrlResponse(firmarUrl(documento.getRutaGcs()), expiracion);
    }

    // Sobrecarga para compatibilidad con código que pasa null como segundo argumento
    @Transactional(readOnly = true)
    public SignedUrlResponse generarSignedUrl(UUID documentoId, Integer usuarioId) {
        return generarSignedUrl(documentoId);
    }

    private String firmarUrl(String rutaGcs) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(gcsBucketName, rutaGcs)).build();
        return storage.signUrl(
                blobInfo,
                SIGNED_URL_MINUTES,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature()
        ).toString();
    }

    // ─── Eliminar ────────────────────────────────────────────────────────────────

    @Transactional
    public void eliminarDocumento(UUID documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado: " + documentoId));

        BlobId blobId = BlobId.of(gcsBucketName, documento.getRutaGcs());
        storage.delete(blobId);
        documentoRepository.delete(documento);
    }

    // Sobrecarga para compatibilidad con código que pasa usuarioId como segundo argumento
    @Transactional
    public void eliminarDocumento(UUID documentoId, Integer usuarioId) {
        eliminarDocumento(documentoId);
    }

    // ─── Crear referencia a un archivo GCS existente (sin re-subir) ──────────────

    @Transactional
    public DocumentoResponse crearReferenciaDocumento(
            String rutaGcs,
            String nombreOriginal,
            String tipoMime,
            String idReferencia,
            String entidadReferencia,
            TipoDocumento tipoDocumento,
            Integer subidoPor
    ) {
        Documento documento = Documento.builder()
                .rutaGcs(rutaGcs)
                .nombreOriginal(nombreOriginal)
                .idReferencia(idReferencia)
                .entidadReferencia(entidadReferencia)
                .tipoDocumento(tipoDocumento)
                .tipoMime(tipoMime)
                .accesoRestringido(true)
                .subidoPor(subidoPor)
                .build();
        return DocumentoResponse.fromEntity(documentoRepository.save(documento));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    // FIX 0000850: tabla de extensiones válidas por cada mime permitido. Se usa
    // para poder rechazar un archivo por su extensión usando solo el nombre del
    // archivo (sin leer ni transferir su contenido), de forma instantánea.
    private static final Map<String, List<String>> EXTENSIONES_POR_MIME = Map.of(
            "application/pdf", List.of("pdf"),
            "image/jpeg", List.of("jpg", "jpeg"),
            "image/png", List.of("png"),
            "image/tiff", List.of("tif", "tiff"),
            "video/mp4", List.of("mp4")
    );

    private void validarArchivo(MultipartFile file, TipoDocumento tipo) {
        if (file.isEmpty()) throw new BusinessException("El archivo no puede estar vacío.");

        TipoDocumentoConfig config = tipoDocumentoConfigRepository.findById(tipo)
                .orElseThrow(() -> new BusinessException("Tipo de documento no configurado: " + tipo));

        List<String> mimesPermitidos = Arrays.asList(config.getMimePermitidos().split(","));

        // FIX 0000850: validamos primero la EXTENSIÓN del archivo. Es la
        // comprobación más barata posible (solo mira el nombre del archivo) y
        // permite devolver el error de inmediato, sin esperar a que termine de
        // "cargarse" el archivo ni de tocar la base de datos o GCS.
        List<String> extensionesPermitidas = mimesPermitidos.stream()
                .flatMap(mime -> EXTENSIONES_POR_MIME.getOrDefault(mime.trim(), List.of()).stream())
                .toList();

        String extension = obtenerExtension(file.getOriginalFilename());
        if (!extensionesPermitidas.contains(extension)) {
            throw new BusinessException("Extensión de archivo no permitida: ." + extension +
                    ". Extensiones permitidas: " + String.join(", ", extensionesPermitidas));
        }

        if (!mimesPermitidos.contains(file.getContentType())) {
            throw new BusinessException("Tipo de archivo no permitido para " + tipo +
                    ". Permitidos: " + config.getMimePermitidos());
        }

        if (file.getSize() > config.getMaxSizeBytes()) {
            long maxMb = config.getMaxSizeBytes() / (1024 * 1024);
            throw new BusinessException("El archivo supera el límite de " + maxMb + " MB.");
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) return "bin";
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase();
    }

    @Transactional
    public void eliminarDocumentosPorReferencia(String entidadReferencia, String idReferencia) {
        // Buscamos todos los documentos asociados a este Reporte (o cualquier entidad)
        List<Documento> documentos = documentoRepository.findByIdReferenciaAndEntidadReferencia(idReferencia, entidadReferencia);

        // Iteramos y reutilizamos tu mé existente que ya se encarga de borrar el Blob en GCS
        for (Documento doc : documentos) {
            eliminarDocumento(doc.getId());
        }
    }
}