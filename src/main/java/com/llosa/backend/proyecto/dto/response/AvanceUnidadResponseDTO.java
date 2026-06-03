package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.HitoUnidad;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.enums.TipoHito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AvanceUnidadResponseDTO(
    UUID id,
    String hitoTitulo,
    Integer hitoOrden,
    TipoHito hitoTipo,
    EstadoHito estado,
    LocalDateTime fechaCompletado
) {
    public static AvanceUnidadResponseDTO fromEntity(HitoUnidad hu) {
        return new AvanceUnidadResponseDTO(
            hu.getId(),
            hu.getHito().getTitulo(),
            hu.getHito().getOrden(),
            hu.getHito().getTipo(),
            hu.getEstado(),
            hu.getFechaCompletado()
        );
    }
}
