package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.Torre;

import java.util.UUID;

public record TorreResponseDTO(
        Long id,
        UUID proyectoId,
        String nombre
) {
    public static TorreResponseDTO fromEntity(Torre t) {
        return new TorreResponseDTO(
                t.getId(),
                t.getProyecto().getId(),
                t.getNombre()
        );
    }
}
