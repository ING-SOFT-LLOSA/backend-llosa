package com.llosa.backend.proyecto.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record HitoCreateDTO(
    @NotBlank String titulo,
    @NotNull Integer orden,
    LocalDate fechaCompletado
) {}
