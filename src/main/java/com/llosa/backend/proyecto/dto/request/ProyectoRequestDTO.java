package com.llosa.backend.proyecto.dto.request;

import java.time.LocalDate;

public record ProyectoRequestDTO(
        String nombre,
        String distrito,
        String direccion,
        LocalDate fechaInicio
) {}
