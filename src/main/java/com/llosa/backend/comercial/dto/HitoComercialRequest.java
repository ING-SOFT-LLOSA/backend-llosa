package com.llosa.backend.comercial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

/**
 * DTO de entrada para crear o editar un hito del proceso de compra comercial.
 */
@Builder
public record HitoComercialRequest(
        @NotNull(message = "El UUID del usuario activo es obligatorio")
        UUID uuidEstapaExpediente,

        @NotBlank(message = "El nombre del hito es obligatorio")
        String nombreHito,

        String descripcion,

        @NotNull(message = "El orden es obligatorio")
        Integer orden
) {}
