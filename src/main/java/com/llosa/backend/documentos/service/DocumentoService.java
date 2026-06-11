package com.llosa.backend.documentos.service;

import com.google.cloud.storage.*;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.dto.StageDocumentResponse;
import com.llosa.backend.documentos.dto.SubirDocumentoRequest;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.entity.TipoDocumentoConfig;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.repository.TipoDocumentoConfigRepository;
import com.llosa.backend.exception.BusinessException;
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
    public StageDocumentResponse obtenerDetalleEtapa(String etapaProceso, UUID uuidUsuarioActivo) {
        if (!"contrato".equalsIgnoreCase(etapaProceso)) {
            return new StageDocumentResponse(null, 0, List.of());
        }

        UsuarioActivo ua = usuarioActivoRepository.findById(uuidUsuarioActivo)
                .orElseThrow(() -> new EntityNotFoundException("UsuarioActivo no encontrado: " + uuidUsuarioActivo));

        List<StageDocumentResponse.DocumentoItemResponse> unidades = new ArrayList<>();
        BigDecimal areaTotal = BigDecimal.ZERO;

        Activo principal = ua.getActivo();
        if (principal != null) {
            unidades.add(mapActivoToItem(principal));
            areaTotal = areaTotal.add(principal.getAreaM2());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
        String firmaContrato = ua.getFechaAdquisicion() != null
                ? ua.getFechaAdquisicion().format(formatter)
                : null;

        String resumen = String.format(Locale.US,
                "Contrato firmado: %s | Financiamiento: %s | Área total: %.2f m²",
                firmaContrato != null ? firmaContrato : "Pendiente",
                ua.getTipoFinanciamiento() != null ? ua.getTipoFinanciamiento() : "-",
                areaTotal);

        return new StageDocumentResponse(resumen, unidades.size(), unidades);
    }

    private StageDocumentResponse.DocumentoItemResponse mapActivoToItem(Activo activo) {
        String tipo = activo.getTipo() == TipoActivo.DEPARTAMENTO ? "DEPARTAMENTO"
                : activo.getTipo() == TipoActivo.COCHERA ? "ESTACIONAMIENTO" : "OTRO";
        String nombre = activo.getTipo() == TipoActivo.DEPARTAMENTO
                ? "Dpto. " + activo.getNro()
                : activo.getTipo() == TipoActivo.COCHERA
                  ? "Cochera " + activo.getNro()
                  : activo.getNro();
        String icono = activo.getTipo() == TipoActivo.DEPARTAMENTO ? "edificio"
                : activo.getTipo() == TipoActivo.COCHERA ? "parking" : "file";

        DecimalFormat df = new DecimalFormat("#,###");
        String aporte = "S/." + df.format(activo.getPrecio());
        String area = String.format(Locale.US, "%.2f m²", activo.getAreaM2());

        return new StageDocumentResponse.DocumentoItemResponse(
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
        String entidad = entidadResolver.resolverEntidad(idReferencia);
        return subirDocumentoPolimorfico(
                idReferencia, file, request.tipoDocumento(),
                idReferencia.toString(), entidad, subidoPor
        );
    }

    // ─── Subir documento (polimórfico — entidad y referencia explícitas) ─────────

    @Transactional
    public DocumentoResponse subirDocumentoPolimorfico(
            UUID usuarioActivoId,
            MultipartFile file,
            TipoDocumento tipoDocumento,
            String idReferencia,
            String entidadReferencia,
            Integer subidoPor
    ) {
        validarArchivo(file, tipoDocumento);

        String extension = obtenerExtension(file.getOriginalFilename());
        String rutaGcs = String.format("proyectos/%s/%s/%s.%s",
                entidadReferencia.toLowerCase(), usuarioActivoId, UUID.randomUUID(), extension);

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

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void validarArchivo(MultipartFile file, TipoDocumento tipo) {
        if (file.isEmpty()) throw new BusinessException("El archivo no puede estar vacío.");

        TipoDocumentoConfig config = tipoDocumentoConfigRepository.findById(tipo)
                .orElseThrow(() -> new BusinessException("Tipo de documento no configurado: " + tipo));

        List<String> mimesPermitidos = Arrays.asList(config.getMimePermitidos().split(","));
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
}