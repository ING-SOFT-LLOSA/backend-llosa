package com.llosa.backend.agenda.dto.request;

import com.llosa.backend.agenda.enums.EstadoCita;

import java.time.LocalDateTime;

/**
 * Payload para actualizar una cita existente desde el Portal Empresa.
 * Todos los campos son opcionales; solo se aplican los que vienen no nulos.
 */
public record ActualizarCitaRequest(
        String titulo,
        String descripcion,
        String ubicacion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        EstadoCita estadoCita,
        Boolean permiteReprogramacion,
        String motivoCancelacion
) {}
