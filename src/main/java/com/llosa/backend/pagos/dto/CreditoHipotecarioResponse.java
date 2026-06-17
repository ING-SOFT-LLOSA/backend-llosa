package com.llosa.backend.pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreditoHipotecarioResponse(
        List<Item> items,
        BigDecimal montoTotal,
        double progreso
) {
    public record Item(
            String nombre,
            LocalDate fecha,
            String estado,
            BigDecimal monto,
            UUID documentId,
            String downloadUrl
    ) {}
}
