package com.llosa.backend.documentos.dto;

import java.util.List;

public record StageDocumentResponse(
        String sectionTitle,
        int totalCount,
        List<DocumentoItemResponse> documents
) {

    public record DocumentoItemResponse(
            String id,
            String title,
            String description,
            String status,
            String emissionDate,
            boolean hasDownload,
            String downloadUrl,
            boolean hasPreview,
            String corporateNoteUrl,
            String icon
    ) {}
}