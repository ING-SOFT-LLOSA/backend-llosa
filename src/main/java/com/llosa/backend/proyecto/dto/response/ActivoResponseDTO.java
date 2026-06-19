package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;

import java.math.BigDecimal;
import java.util.UUID;

public record ActivoResponseDTO(
        UUID id,
        Long pisoId,
        Integer nroPiso,
        String torreNombre,
        String proyectoNombre,
        String nro,
        TipoActivo tipo,
        BigDecimal areaM2,
        BigDecimal areaTechada,
        EstadoComercialActivo estadoComercial,
        BigDecimal precio,
        String descripcion
) {
    public static ActivoResponseDTO fromEntity(Activo a) {
        return new ActivoResponseDTO(
                a.getId(),
                a.getPiso().getId(),
                a.getPiso().getNroPiso(),
                a.getPiso().getTorre().getNombre(),
                a.getPiso().getTorre().getProyecto().getNombre(),
                a.getNro(),
                a.getTipo(),
                a.getAreaM2(),
                a.getAreaTechada(),
                a.getEstadoComercial(),
                a.getPrecio(),
                a.getDescripcion()
        );
    }
}
