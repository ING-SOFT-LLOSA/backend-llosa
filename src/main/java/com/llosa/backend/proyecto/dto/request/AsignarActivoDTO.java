package com.llosa.backend.proyecto.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AsignarActivoDTO(
        @NotNull Integer idUsuario,
        @NotNull UUID idActivo,
        String tipoFinanciamiento,
        String faseComercial,
        String estadoTramiteLegal,
        LocalDateTime fechaAdquisicion
) {}
