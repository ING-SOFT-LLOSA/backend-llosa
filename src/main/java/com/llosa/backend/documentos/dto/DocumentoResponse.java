package com.llosa.backend.documentos.dto;

import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.enums.TipoDocumento;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentoResponse(
        UUID id,
        String nombreOriginal,
        TipoDocumento tipoDocumento,
        String tipoMime,
        String idReferencia,
        String entidadReferencia,
        LocalDateTime createdAt,
        String urlAcceso
) {
    /** Uso general: sin URL firmada (urlAcceso = null). */
    public static DocumentoResponse fromEntity(Documento doc) {
        return new DocumentoResponse(
                doc.getId(),
                doc.getNombreOriginal(),
                doc.getTipoDocumento(),
                doc.getTipoMime(),
                doc.getIdReferencia(),
                doc.getEntidadReferencia(),
                doc.getCreatedAt(),
                null
        );
    }

    /** Con URL firmada lista para renderizar en el frontend. */
    public static DocumentoResponse fromEntity(Documento doc, String urlAcceso) {
        return new DocumentoResponse(
                doc.getId(),
                doc.getNombreOriginal(),
                doc.getTipoDocumento(),
                doc.getTipoMime(),
                doc.getIdReferencia(),
                doc.getEntidadReferencia(),
                doc.getCreatedAt(),
                urlAcceso
        );
    }
}
