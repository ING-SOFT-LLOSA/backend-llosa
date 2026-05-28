package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.response.SeguimientoResponseDTO;

import java.util.UUID;

public interface SeguimientoService {
    SeguimientoResponseDTO obtenerSeguimiento(UUID uuidActivo);
}
