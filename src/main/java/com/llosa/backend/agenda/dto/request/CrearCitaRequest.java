package com.llosa.backend.agenda.dto.request;

import com.llosa.backend.agenda.enums.TipoEvento;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload para crear una nueva cita desde el Portal Empresa.
 * Solo personal autorizado (POSTVENTA, ASESOR, ADMIN) puede crear citas.
 */
public record CrearCitaRequest(

        @NotNull(message = "El ID del cliente es obligatorio")
        Integer clienteId,

        @NotNull(message = "El ID del activo (unidad) es obligatorio")
        UUID activoId,

        @NotNull(message = "El tipo de evento es obligatorio")
        TipoEvento tipoEvento,

        String titulo,

        String descripcion,

        String ubicacion,

        @NotNull(message = "La fecha de inicio es obligatoria")
        @Future(message = "La cita debe ser en una fecha futura")
        LocalDateTime fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDateTime fechaFin,

        /** true → el cliente podrá proponer horarios alternativos. */
        boolean permiteReprogramacion,

        /**
         * Indica si el cliente tiene cuenta Google.
         * true  → se intentará sincronizar con Google Calendar.
         * false → la cita solo se persiste en el backend.
         */
        boolean clienteUsaGoogle
) {}
