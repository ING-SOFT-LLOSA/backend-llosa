package com.llosa.backend.proyecto.dto.request;

import java.util.List;

public record ProyectoCargaDTO(
        List<TorreRequestDTO> torres
) { }
