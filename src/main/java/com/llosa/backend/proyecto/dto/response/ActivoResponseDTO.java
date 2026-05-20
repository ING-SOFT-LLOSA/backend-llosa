package com.llosa.backend.proyecto.dto.response;


import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.enums.TipoActivo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActivoResponseDTO(
        UUID id,
        Long pisoId,
        String nro,
        TipoActivo tipo,
        BigDecimal precio,
        String descripcion,
        LocalDateTime createdAt
) {
    public static ActivoResponseDTO fromEntity(Activo a) {
        return new ActivoResponseDTO(
                a.getId(),
                a.getPiso().getId(),
                a.getNro(),
                a.getTipo(),
                a.getPrecio(),
                a.getDescripcion(),
                a.getCreatedAt()
        );
    }
}
