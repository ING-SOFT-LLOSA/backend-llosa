package com.llosa.backend.proyecto.dto.request;

import java.util.List;

public record PisoRequestDTO(
        String nroPiso,
        List<ActivoRequestDTO> activos
) { }
