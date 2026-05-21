package com.llosa.backend.proyecto.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ProyectoCreateDTO(
        @NotBlank String nombre,
        String descripcion,
        Boolean precertificacionEdgeLeed,
        String linkRecorridoVirtual,
        String departamento,
        String distrito,
        String direccion,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {}
