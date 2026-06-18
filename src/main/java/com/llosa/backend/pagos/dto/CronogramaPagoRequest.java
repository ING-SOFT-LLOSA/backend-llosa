package com.llosa.backend.pagos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CronogramaPagoRequest(
        @NotNull UUID uuidUsuarioActivo,
        @NotNull @Positive BigDecimal totalPactado,
        Integer numeroCuotas,
        BigDecimal pagoSeparacion,
        BigDecimal pagoIncial
) {}
