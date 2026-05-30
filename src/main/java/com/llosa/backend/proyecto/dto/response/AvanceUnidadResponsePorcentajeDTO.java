package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.HitoUnidad;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.enums.TipoHito;

import java.time.LocalDate;
import java.util.UUID;

public record AvanceUnidadResponsePorcentajeDTO(
        UUID id,
        String hitoTitulo,
        Integer hitoOrden,
        TipoHito hitoTipo,
        EstadoHito estado,
        LocalDate fechaCompletado,
        Integer porcentaje
) {
    public static AvanceUnidadResponsePorcentajeDTO fromEntity(HitoUnidad hu, Integer porcentaje) {
        return new AvanceUnidadResponsePorcentajeDTO(
                hu.getId(),
                hu.getHito().getTitulo(),
                hu.getHito().getOrden(),
                hu.getHito().getTipo(),
                hu.getEstado(),
                hu.getFechaCompletado(),
                porcentaje
        );
    }
}
