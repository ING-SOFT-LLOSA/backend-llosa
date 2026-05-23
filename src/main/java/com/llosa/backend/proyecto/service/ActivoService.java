package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Activo;

public interface ActivoService {
    Activo save(Long pisoId, Activo activo);
}
