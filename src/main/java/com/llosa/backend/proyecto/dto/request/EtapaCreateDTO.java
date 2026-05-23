package com.llosa.backend.proyecto.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EtapaCreateDTO(
    @NotBlank String nombre,
    String descripcion,
    @NotNull Integer orden
) {}
