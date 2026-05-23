package com.llosa.backend.proyecto.dto.response;

import java.util.UUID;

public record DashboardProyectoDTO(
    UUID proyectoId,
    String nombre,
    double porcentajeAvance
) {}
