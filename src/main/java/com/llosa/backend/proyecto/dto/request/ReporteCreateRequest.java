package com.llosa.backend.proyecto.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReporteCreateRequest(

        @NotNull(message = "El UUID del proyecto es obligatorio")
        UUID uuidProyecto,

        @NotBlank(message = "El título del período no puede estar vacío")
        String tituloPeriodo,

        String descripcion,

        LocalDate fecha,

        List<String> hitosConsolidados
) {}
