package com.llosa.backend.proyecto.dto.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PasoStepperDTO(
        String nombre,
        String estado,
        int orden
){
}
