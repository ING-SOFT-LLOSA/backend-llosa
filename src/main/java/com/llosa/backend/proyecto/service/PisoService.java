package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Piso;

public interface PisoService {
    Piso save(Long torreId, Piso piso);
    Piso findById(Long id);
}
