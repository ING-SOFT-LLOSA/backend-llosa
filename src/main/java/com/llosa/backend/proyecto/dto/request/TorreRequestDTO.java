package com.llosa.backend.proyecto.dto.request;

import java.util.List;

public record TorreRequestDTO(
    String nombre,
    List<PisoRequestDTO> pisos
) {
}
