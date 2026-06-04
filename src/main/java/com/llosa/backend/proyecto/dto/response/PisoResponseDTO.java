package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.Piso;

public record PisoResponseDTO(
        Long id,
        Integer nroPiso
) {
    public static PisoResponseDTO fromEntity(Piso piso) {
        return new PisoResponseDTO(
                piso.getId(),
                piso.getNroPiso()
        );
    }
}
