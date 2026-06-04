package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Piso;
import java.util.List;

public interface PisoService {
    Piso save(Long torreId, Piso piso);
    Piso findById(Long id);
    List<Piso> findByTorre(Long torreId, String search);
}
