package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;


import java.math.BigDecimal;
import java.util.UUID;

public record MisActivosResponseDTO(
        UUID id,
        Long pisoId,
        Integer nroPiso,
        String torreNombre,
        String proyectoNombre,
        String direccion,
        String distrito,
        String nro,
        TipoActivo tipo,
        BigDecimal areaM2,
        EstadoComercialActivo estadoComercial,
        BigDecimal precio,
        String descripcion
) {
    public static MisActivosResponseDTO fromEntity(Activo a) {
        Proyecto proyecto = a.getPiso().getTorre().getProyecto();

        return new MisActivosResponseDTO(
                a.getId(),
                a.getPiso().getId(),
                a.getPiso().getNroPiso(),
                a.getPiso().getTorre().getNombre(),
                proyecto.getNombre(),
                proyecto.getDireccion(),
                proyecto.getDistrito(),
                a.getNro(),
                a.getTipo(),
                a.getAreaM2(),
                a.getEstadoComercial(),
                a.getPrecio(),
                a.getDescripcion()
        );
    }
}
