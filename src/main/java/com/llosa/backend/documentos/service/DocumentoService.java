package com.llosa.backend.documentos.service;

import com.google.cloud.storage.*;
import com.llosa.backend.documentos.dto.*;
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
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
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
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Storage storage;
    private final String gcsBucketName;

    @Transactional(readOnly = true)
    public StageResponse obtenerDetalleEtapa(String etapaProceso, UUID uuidUsuarioActivo) {
        if (!"contrato".equalsIgnoreCase(etapaProceso)) {
            return new StageResponse(null);
        }

        UsuarioActivo ua = usuarioActivoRepository.findById(uuidUsuarioActivo)
                .orElseThrow(() -> new EntityNotFoundException("UsuarioActivo no encontrado: " + uuidUsuarioActivo));

        List<StageResponse.UnidadResponse> unidades = new ArrayList<>();
        BigDecimal areaTotal = BigDecimal.ZERO;
        int departamentos = 0;
        int estacionamientos = 0;

        // Departamento principal
        Activo principal = ua.getActivo();
        if (principal != null) {
            unidades.add(mapActivoToUnidad(principal));
            areaTotal = areaTotal.add(principal.getAreaM2());
            if (principal.getTipo() == TipoActivo.DEPARTAMENTO) {
                departamentos++;
            } else if (principal.getTipo() == TipoActivo.COCHERA) {
                estacionamientos++;
            }
        }

        // Cochera opcional
        Activo cochera = ua.getCochera();
        if (cochera != null) {
            unidades.add(mapActivoToUnidad(cochera));
            areaTotal = areaTotal.add(cochera.getAreaM2());
            estacionamientos++;
        }

        StageResponse.ResumenContratoResponse resumen = new StageResponse.ResumenContratoResponse(
                unidades.size(),
                String.format(Locale.US, "%.2f m²", areaTotal),
                unidades,
                new StageResponse.TotalesResponse(departamentos, estacionamientos)
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
        String firmaContrato = ua.getFechaAdquisicion() != null ? ua.getFechaAdquisicion().format(formatter) : null;

        StageResponse.InformacionContratoResponse info = new StageResponse.InformacionContratoResponse(
                firmaContrato,
                null,
                ua.getTipoFinanciamiento()
        );

        return new StageResponse(new StageResponse.StageDetailsResponse(resumen, info));
    }

    private StageResponse.UnidadResponse mapActivoToUnidad(Activo activo) {
        String tipo = "OTRO";
        String nombre = activo.getNro();
        String icono = "file";
        String areaOcupada = null;

        if (activo.getTipo() == TipoActivo.DEPARTAMENTO) {
            tipo = "DEPARTAMENTO";
            nombre = "Dpto. " + activo.getNro();
            icono = "edificio";
            areaOcupada = String.format(Locale.US, "%.2f m²", activo.getAreaM2());
        } else if (activo.getTipo() == TipoActivo.COCHERA) {
            tipo = "ESTACIONAMIENTO";
            nombre = "Cochera " + activo.getNro();
            icono = "parking";
            areaOcupada = null;
        }

        DecimalFormat df = new DecimalFormat("#,###");
        String aporte = "S/." + df.format(activo.getPrecio());

        return new StageResponse.UnidadResponse(
                tipo,
                nombre,
                aporte,
                areaOcupada,
                String.format(Locale.US, "%.2f m²", activo.getAreaM2()),
                icono
        );
    }

    @Transactional
    public DocumentoResponse subirDocumento(UUID usuarioActivoId, MultipartFile file, SubirDocumentoRequest request, Integer subidoPor) {

        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(usuarioActivoId)
                .orElseThrow(() -> new EntityNotFoundException("UsuarioActivo no encontrado: " + usuarioActivoId));

        validarArchivo(file, request.tipoDocumento());

        String extension = obtenerExtension(file.getOriginalFilename());
        UUID uuidArchivo = UUID.randomUUID();
        String rutaGcs = String.format("expedientes/%s/%s.%s",
                usuarioActivoId, uuidArchivo, extension);

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
                .idReferencia(usuarioActivoId.toString())
                .entidadReferencia("USUARIO_ACTIVO")
                .tipoDocumento(request.tipoDocumento())
                .tipoMime(file.getContentType())
                .accesoRestringido(true)
                .subidoPor(subidoPor)
                .build();

        return DocumentoResponse.fromEntity(documentoRepository.save(documento));
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listarPorUsuarioActivo(UUID usuarioActivoId, TipoDocumento tipoDocumento) {
        if (!usuarioActivoRepository.existsById(usuarioActivoId)) {
            throw new EntityNotFoundException("UsuarioActivo no encontrado: " + usuarioActivoId);
        }

        List<Documento> documentos = tipoDocumento != null
                ? documentoRepository.findByIdReferenciaAndEntidadReferenciaAndTipoDocumento(
                usuarioActivoId.toString(), "USUARIO_ACTIVO", tipoDocumento)
                : documentoRepository.findByIdReferenciaAndEntidadReferencia(
                usuarioActivoId.toString(), "USUARIO_ACTIVO");

        return documentos.stream().map(DocumentoResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public StageDocumentResponse listarDocumentosCliente(String firebaseUid) {
        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        List<UsuarioActivo> expedientes = usuarioActivoRepository.findByClienteId(usuario.getId());

        if (expedientes.isEmpty()) {
            throw new EntityNotFoundException("El usuario no tiene unidades asignadas");
        }

        List<Documento> documentos = expedientes.stream()
                .flatMap(ua -> documentoRepository
                        .findByIdReferenciaAndEntidadReferencia(
                                ua.getUuidUsuarioActivo().toString(),
                                "USUARIO_ACTIVO"
                        ).stream()
                )
                .toList();

        List<StageDocumentResponse.DocumentoItemResponse> items = documentos.stream()
                .map(doc -> new StageDocumentResponse.DocumentoItemResponse(
                        doc.getId().toString(),
                        doc.getNombreOriginal(),
                        doc.getTipoDocumento().name(),
                        doc.getCreatedAt() != null ? "completed" : "pending",
                        doc.getCreatedAt() != null ? doc.getCreatedAt().toLocalDate().toString() : null,
                        true,
                        "/api/documentos/" + doc.getId() + "/signed-url",
                        true,
                        null,
                        resolverIcono(doc.getTipoDocumento())
                ))
                .toList();

        return new StageDocumentResponse(
                "Documentos de este modulo",
                items.size(),
                items
        );
    }

    @Transactional(readOnly = true)
    public SignedUrlResponse generarSignedUrl(UUID documentoId, Integer usuarioId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado: " + documentoId));

        BlobInfo blobInfo = BlobInfo.newBuilder(
                BlobId.of(gcsBucketName, documento.getRutaGcs())
        ).build();

        Instant expiracion = Instant.now().plusSeconds(SIGNED_URL_MINUTES * 60);

        String url = storage.signUrl(
                blobInfo,
                SIGNED_URL_MINUTES,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature()
        ).toString();

        return new SignedUrlResponse(url, expiracion);
    }

    @Transactional
    public void eliminarDocumento(UUID documentoId, Integer usuarioId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado: " + documentoId));

        BlobId blobId = BlobId.of(gcsBucketName, documento.getRutaGcs());
        storage.delete(blobId);

        documentoRepository.delete(documento);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void validarArchivo(MultipartFile file, TipoDocumento tipo) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo no puede estar vacío.");
        }

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

    private String resolverIcono(TipoDocumento tipo) {
        return switch (tipo) {
            case PDF_LEGAL -> "contract";
            case COMPROBANTE -> "bank";
            case FOTO_OBRA -> "clipboard";
            case VIDEO_OBRA -> "file";
        };
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) return "bin";
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase();
    }
}