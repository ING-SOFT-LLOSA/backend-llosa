package com.llosa.backend.proyecto.dto.response;


import com.llosa.backend.proyecto.entity.Proyecto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProyectoResponseDTO(
        UUID id,
        String nombre,
        String descripcion,
        Boolean precertificacionEdgeLeed,
        String departamento,
        String distrito,
        String direccion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        LocalDateTime createdAt
) {
    public static ProyectoResponseDTO fromEntity(Proyecto p) {
        return new ProyectoResponseDTO(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecertificacionEdgeLeed(),
                p.getDepartamento(),
                p.getDistrito(),
                p.getDireccion(),
                p.getFechaInicio(),
                p.getFechaFin(),
                p.getCreatedAt()
        );
    }
}
