package com.llosa.backend.pagos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CartaAprobacionRequest(
        @NotNull UUID uuidUsuarioActivo,
        @NotBlank String banco,
        @NotNull @Positive BigDecimal montoAprobado,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        LocalDate fechaDesembolsoProyectada,
        BigDecimal pagoSeparacion,
        BigDecimal pagoInicial,
        Boolean pagoCompleto,
        String comentarios
) {}
