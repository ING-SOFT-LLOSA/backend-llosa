package com.llosa.backend.proyecto.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.llosa.backend.proyecto.dto.shared.FaseActualDTO;
import com.llosa.backend.proyecto.dto.shared.PasoStepperDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record SeguimientoResponseDTO(
        List<PasoStepperDTO> stepper,
        @JsonProperty("fase_actual") FaseActualDTO faseActual
) {
}
