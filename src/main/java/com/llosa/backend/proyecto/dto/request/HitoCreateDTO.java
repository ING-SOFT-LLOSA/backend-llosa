package com.llosa.backend.proyecto.dto.request;

import com.llosa.backend.proyecto.enums.TipoHito;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record HitoCreateDTO(
    @NotBlank String titulo,
    String nombre,
    String descripcion,
    @NotNull Integer orden,
    @NotNull TipoHito tipo,
    LocalDate fechaEstimada
) {}
