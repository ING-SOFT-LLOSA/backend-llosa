package com.llosa.backend.proyecto.dto.response;


import com.llosa.backend.proyecto.entity.Piso;

public record PisoResponseDTO(
        Long id,
        Long torreId,
        Integer nroPiso
) {
    public static PisoResponseDTO fromEntity(Piso p) {
        return new PisoResponseDTO(
                p.getId(),
                p.getTorre().getId(),
                p.getNroPiso()
        );
    }
}
