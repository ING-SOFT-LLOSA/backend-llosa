package com.llosa.backend.comercial.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record RequisitoUpdateRequest(
        @NotBlank String titulo,
        String descripcion,
        String notaCorporativa,
        String estado, // "PENDIENTE", "COMPLETADA", "RECHAZADO"
        LocalDate fechaEmision,
        String icono
) {}