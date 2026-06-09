package com.llosa.backend.comercial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


import java.time.LocalDate;
import java.util.UUID;

public record RequisitoCreateRequest(
        @NotNull UUID hitoProcesoCompraId,
        @NotBlank String titulo,
        String descripcion,
        String notaCorporativa,
        LocalDate fechaEmision,
        String icono
) {}