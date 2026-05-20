package com.llosa.backend.proyecto.dto.response;


import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.enums.EstadoEtapa;

import java.util.UUID;

public record EtapaResponseDTO(
        Long id,
        UUID proyectoId,
        String nombre,
        String descripcion,
        EstadoEtapa estado
) {
    public static EtapaResponseDTO fromEntity(Etapa e) {
        return new EtapaResponseDTO(
                e.getId(),
                e.getProyecto().getId(),
                e.getNombre(),
                e.getDescripcion(),
                e.getEstado()
        );
    }
}
