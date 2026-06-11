package com.llosa.backend.comercial.dto;

import java.util.List;

public record StageDocumentsResponse(
        String title,
        int totalCount,
        List<DocumentoItem> documents
) {
    public record DocumentoItem(
            String id,
            String title,
            String description,
            String status,
            String emissionDate,
            boolean hasDownload,
            String downloadUrl,
            boolean hasPreview,
            String notaCorporativa,
            String icon
    ) {}
}
