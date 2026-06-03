package com.llosa.backend.proyecto.dto.comercial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO raíz del stepper de proceso de compra.
 * Agrupa todas las etapas con sus hitos para un UsuarioActivo específico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepperResponse {

    private UUID uuidUsuarioActivo;
    private List<EtapaStepperResponse> etapas;
}
