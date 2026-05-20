package com.llosa.backend.proyecto.dto.response;


import com.llosa.backend.proyecto.entity.Proyecto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProyectoResponseDTO(
        UUID id,
        String nombre,
        String distrito,
        String direccion,
        LocalDate fechaInicio,
        LocalDateTime createdAt
) {
    public static ProyectoResponseDTO fromEntity(Proyecto p) {
        return new ProyectoResponseDTO(
                p.getId(),
                p.getNombre(),
                p.getDistrito(),
                p.getDireccion(),
                p.getFechaInicio(),
                p.getCreatedAt()
        );
    }
}
