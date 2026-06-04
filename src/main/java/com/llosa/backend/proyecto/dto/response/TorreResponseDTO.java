package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.Torre;

public record TorreResponseDTO(
        Long id,
        String nombre
) {
    public static TorreResponseDTO fromEntity(Torre torre) {
        return new TorreResponseDTO(
                torre.getId(),
                torre.getNombre()
        );
    }
}
