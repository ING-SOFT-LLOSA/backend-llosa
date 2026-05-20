package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Piso;

import java.util.List;

public interface PisoService {

    Piso save(Long torreId, Piso piso);

    Piso findById(Long id);

    List<Piso> findByTorreId(Long torreId);

    Piso update(Long id, Piso datos);

    void delete(Long id);
}
