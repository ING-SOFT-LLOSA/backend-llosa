package com.llosa.backend.proyecto.dto.request;

import com.llosa.backend.proyecto.enums.EstadoHito;
import jakarta.validation.constraints.NotNull;

public record HitoUnidadUpdateDTO(
    @NotNull EstadoHito estado
) {}
