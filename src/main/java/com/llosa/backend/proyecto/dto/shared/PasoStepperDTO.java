package com.llosa.backend.proyecto.dto.shared;

import lombok.Builder;

@Builder
public record PasoStepperDTO(
        String nombre,
        String estado,
        int orden
){
}
