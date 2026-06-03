package com.llosa.backend.proyecto.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * DTO raíz del stepper de proceso de compra.
 * Agrupa todas las etapas con sus hitos para un UsuarioActivo específico.
 */
@Builder
public record StepperResponse(
    UUID uuidUsuarioActivo,
    List<EtapaStepperResponse> etapas
) {}
