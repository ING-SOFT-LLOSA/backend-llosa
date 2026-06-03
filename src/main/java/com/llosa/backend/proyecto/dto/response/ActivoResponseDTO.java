package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;

import java.math.BigDecimal;
import java.util.UUID;

public record ActivoResponseDTO(
        UUID id,
        Long pisoId,
        String nro,
        TipoActivo tipo,
        BigDecimal areaM2,
        EstadoComercialActivo estadoComercial,
        BigDecimal precio,
        String descripcion
) {
    public static ActivoResponseDTO fromEntity(Activo a) {
        return new ActivoResponseDTO(
                a.getId(),
                a.getPiso().getId(),
                a.getNro(),
                a.getTipo(),
                a.getAreaM2(),
                a.getEstadoComercial(),
                a.getPrecio(),
                a.getDescripcion()
        );
    }
}
