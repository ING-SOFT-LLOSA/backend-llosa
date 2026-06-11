package com.llosa.backend.agenda.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payload para que el cliente proponga bloques de disponibilidad (When2meet).
 * Endpoint: POST /api/agenda/citas/{id}/disponibilidad
 */
public record DisponibilidadRequest(

        @NotEmpty(message = "Debes proponer al menos un bloque horario")
        List<BloqueHorario> bloques
) {
    public record BloqueHorario(
            @NotNull LocalDateTime inicio,
            @NotNull LocalDateTime fin
    ) {}
}
