package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.enums.EstadoHito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record HitoPisoResponseDTO(
        UUID id,
        String nombre,
        EstadoHito estado,
        LocalDate fechaCompletado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String observaciones
) {
    public static HitoPisoResponseDTO fromEntity(HitoPiso hu) {
        return new HitoPisoResponseDTO(
                hu.getId(),
                hu.getHito().getTitulo(),
                hu.getEstado(),
                hu.getFechaCompletado(),
                hu.getCreatedAt(),
                hu.getUpdatedAt(),
                hu.getObservaciones()
        );
    }
}
