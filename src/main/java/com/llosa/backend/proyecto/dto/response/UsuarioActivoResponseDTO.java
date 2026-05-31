package com.llosa.backend.proyecto.dto.response;


import com.llosa.backend.proyecto.entity.UsuarioActivo;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioActivoResponseDTO(
        UUID uuidUsuarioActivo,
        String tipoFinanciamiento,
        String faseComercial,
        String estadoTramiteLegal,
        LocalDateTime fechaAdquisicion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UsuarioActivoResponseDTO fromEntity(UsuarioActivo a){
        return new UsuarioActivoResponseDTO(
                a.getUuidUsuarioActivo(),
                a.getTipoFinanciamiento(),
                a.getFaseComercial(),
                a.getEstadoTramiteLegal(),
                a.getFechaAdquisicion(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
