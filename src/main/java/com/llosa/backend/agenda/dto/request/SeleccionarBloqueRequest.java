package com.llosa.backend.agenda.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Payload para que el gestor seleccione un bloque de disponibilidad propuesto por el cliente.
 * Endpoint: PATCH /api/agenda/citas/{id}/seleccionar-bloque
 */
public record SeleccionarBloqueRequest(

        @NotNull(message = "El ID del bloque es obligatorio")
        Long bloqueId
) {}
