package com.llosa.backend.proyecto.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public record CrearContratoDTO(
        @NotEmpty List<Integer> idsUsuarios, // Los compradores/esposos/socios iniciales
        @NotBlank String tipoFinanciamiento,
        @NotBlank String faseComercial,
        String estadoTramiteLegal,
        LocalDateTime fechaAdquisicion,
        LocalDateTime fechaCompletado
) {}