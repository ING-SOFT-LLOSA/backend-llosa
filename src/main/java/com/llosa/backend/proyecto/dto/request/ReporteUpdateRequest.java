package com.llosa.backend.proyecto.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record ReporteUpdateRequest(

        @NotBlank(message = "El título del período no puede estar vacío")
        String tituloPeriodo,

        String descripcion,

        LocalDate fecha,

        List<String> hitosConsolidados
) {}
