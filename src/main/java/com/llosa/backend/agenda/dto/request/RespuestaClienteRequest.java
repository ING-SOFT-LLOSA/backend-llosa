package com.llosa.backend.agenda.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Payload para que el cliente confirme o decline una cita.
 * Endpoint: PATCH /api/agenda/citas/{id}/respuesta
 */
public record RespuestaClienteRequest(

        @NotNull(message = "Debes indicar si confirmas o declines la cita")
        Boolean confirmado,

        /** Mensaje opcional del cliente al confirmar/declinar. */
        String nota
) {}
