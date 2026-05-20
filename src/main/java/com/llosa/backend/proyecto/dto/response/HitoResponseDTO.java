package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.enums.TipoHito;

import java.time.LocalDate;
import java.util.UUID;

public record HitoResponseDTO(
        UUID id,
        Long etapaId,
        Integer orden,
        TipoHito tipo,
        String titulo,
        String nombre,
        String descripcion,
        EstadoHito estado,
        LocalDate fechaEstimada
) {
    public static HitoResponseDTO fromEntity(Hito h) {
        return new HitoResponseDTO(
                h.getId(),
                h.getEtapa().getId(),
                h.getOrden(),
                h.getTipo(),
                h.getTitulo(),
                h.getNombre(),
                h.getDescripcion(),
                h.getEstado(),
                h.getFechaEstimada()
        );
    }
}
