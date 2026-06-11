package com.llosa.backend.comercial.dto;

import jakarta.validation.constraints.NotBlank;

public record RequisitoUpdateRequest(
        @NotBlank String titulo,
        String descripcion,
        String notaCorporativa,
        String estado // "PENDIENTE", "COMPLETADA", "RECHAZADO"
) {}