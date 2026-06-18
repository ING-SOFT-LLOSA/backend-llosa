package com.llosa.backend.pagos.dto;

import com.llosa.backend.pagos.ConceptoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoRequest(
        @NotNull Integer nroCuota,
        @NotNull @Positive BigDecimal montoProgramado,
        @NotNull LocalDate fechaVencimiento,
        ConceptoPago concepto,
        String comentario
) {}
