package com.llosa.backend.documentos.service;

import com.google.cloud.storage.*;
import com.llosa.backend.documentos.dto.SubirDocumentoRequest;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.repository.HitoUnidadRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private static final long MAX_SIZE_PDF   = 20 * 1024 * 1024L;
    private static final long MAX_SIZE_VIDEO = 50 * 1024 * 1024L;
    private static final long SIGNED_URL_MINUTES = 15L;

    private final DocumentoRepository documentoRepository;
    private final HitoUnidadRepository hitoUnidadRepository;
    private final Storage storage;
    private final String gcsBucketName;

    @Transactional
    public DocumentoResponse subirDocumento(UUID hitoUnidadId, MultipartFile file, SubirDocumentoRequest request, Integer subidoPor) {

        var hitoUnidad = hitoUnidadRepository.findById(hitoUnidadId)
                .orElseThrow(() -> new EntityNotFoundException("HitoUnidad no encontrado: " + hitoUnidadId));

        validarArchivo(file, request.tipoDocumento());

        UUID activoId = hitoUnidad.getActivo().getId();
        String extension = obtenerExtension(file.getOriginalFilename());
        UUID uuidArchivo = UUID.randomUUID();
        String rutaGcs = String.format("unidades/%s/hitos/%s/%s.%s",
                activoId, hitoUnidadId, uuidArchivo, extension);

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
                .idReferencia(hitoUnidadId.toString())
                .entidadReferencia("HITO_UNIDAD")
                .tipoDocumento(request.tipoDocumento())
                .tipoMime(file.getContentType())
                .accesoRestringido(true)
                .subidoPor(subidoPor)
                .build();

        return DocumentoResponse.fromEntity(documentoRepository.save(documento));
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listarPorHitoUnidad(UUID hitoUnidadId) {
        if (!hitoUnidadRepository.existsById(hitoUnidadId)) {
            throw new EntityNotFoundException("HitoUnidad no encontrado: " + hitoUnidadId);
        }
        return documentoRepository
                .findByIdReferenciaAndEntidadReferencia(hitoUnidadId.toString(), "HITO_UNIDAD")
                .stream()
                .map(DocumentoResponse::fromEntity)
                .toList();
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

        switch (tipo) {
            case PDF_LEGAL -> {
                if (!"application/pdf".equals(file.getContentType())) {
                    throw new BusinessException("Solo se permiten archivos PDF para documentos legales.");
                }
                if (file.getSize() > MAX_SIZE_PDF) {
                    throw new BusinessException("El archivo supera el límite de 20 MB.");
                }
            }
            case COMPROBANTE -> {
                List<String> tiposPermitidos = List.of("application/pdf", "image/jpeg", "image/png");
                if (!tiposPermitidos.contains(file.getContentType())) {
                    throw new BusinessException("Solo se permiten PDF, JPG o PNG para comprobantes.");
                }
                if (file.getSize() > MAX_SIZE_PDF) {
                    throw new BusinessException("El archivo supera el límite de 20 MB.");
                }
            }
            case FOTO_OBRA -> {
                List<String> tiposPermitidos = List.of("image/jpeg", "image/png", "image/tiff");
                if (!tiposPermitidos.contains(file.getContentType())) {
                    throw new BusinessException("Solo se permiten JPG, PNG o TIFF para fotos de obra.");
                }
                if (file.getSize() > MAX_SIZE_PDF) {
                    throw new BusinessException("El archivo supera el límite de 20 MB.");
                }
            }
            case VIDEO_OBRA -> {
                if (!"video/mp4".equals(file.getContentType())) {
                    throw new BusinessException("Solo se permiten archivos MP4 para videos de obra.");
                }
                if (file.getSize() > MAX_SIZE_VIDEO) {
                    throw new BusinessException("El archivo supera el límite de 50 MB.");
                }
            }
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "bin";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase();
    }
}