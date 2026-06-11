package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.enums.EstadoHito;

import java.time.LocalDate;
import java.util.UUID;

public record AvanceUnidadResponsePorcentajeDTO(
        UUID id,
        String hitoTitulo,
        Integer hitoOrden,
        EstadoHito estado,
        LocalDate fechaCompletado,
        Integer porcentaje
) {
    public static AvanceUnidadResponsePorcentajeDTO fromEntity(HitoPiso hu, Integer porcentaje) {
        return new AvanceUnidadResponsePorcentajeDTO(
                hu.getId(),
                hu.getHito().getTitulo(),
                hu.getHito().getOrden(),
                hu.getEstado(),
                hu.getFechaCompletado(),
                porcentaje
        );
    }
}
