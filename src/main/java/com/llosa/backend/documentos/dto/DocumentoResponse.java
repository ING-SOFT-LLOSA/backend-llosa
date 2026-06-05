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
        LocalDateTime createdAt
) {
    public static DocumentoResponse fromEntity(Documento doc) {
        return new DocumentoResponse(
                doc.getId(),
                doc.getNombreOriginal(),
                doc.getTipoDocumento(),
                doc.getTipoMime(),
                doc.getIdReferencia(),
                doc.getEntidadReferencia(),
                doc.getCreatedAt()
        );
    }
}