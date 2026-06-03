package com.llosa.backend.proyecto.dto.request;

import java.util.List;

public record PisoRequestDTO(
        Integer nroPiso,
        List<ActivoRequestDTO> activos
) { }
