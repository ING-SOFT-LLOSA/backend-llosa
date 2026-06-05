package com.llosa.backend.documentos.dto;

import java.time.Instant;

public record SignedUrlResponse(
        String url,
        Instant expiracion
) {}