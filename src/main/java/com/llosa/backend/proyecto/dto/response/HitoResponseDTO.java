package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.enums.EstadoHito;

import java.time.LocalDate;
import java.util.UUID;

public record HitoResponseDTO(
        UUID id,
        UUID proyectoId,
        Integer orden,
        String titulo,
        EstadoHito estado,
        LocalDate fechaCompletado
) {
    public static HitoResponseDTO fromEntity(Hito h) {
        return new HitoResponseDTO(
                h.getId(),
                h.getProyecto().getId(),
                h.getOrden(),
                h.getTitulo(),
                h.getEstado(),
                h.getFechaCompletado()
        );
    }
}
