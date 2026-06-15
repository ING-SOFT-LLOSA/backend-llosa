package com.llosa.backend.agenda.dto.response;

import com.llosa.backend.agenda.entity.DisponibilidadCita;

import java.time.LocalDateTime;

public record DisponibilidadResponse(
        Long id,
        LocalDateTime bloqueInicio,
        LocalDateTime bloqueFin,
        Boolean seleccionado,
        LocalDateTime createdAt
) {
    public static DisponibilidadResponse fromEntity(DisponibilidadCita d) {
        return new DisponibilidadResponse(
                d.getId(),
                d.getBloqueInicio(),
                d.getBloqueFin(),
                d.getSeleccionado(),
                d.getCreatedAt()
        );
    }
}
