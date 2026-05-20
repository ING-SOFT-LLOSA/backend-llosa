package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Torre;

import java.util.List;
import java.util.UUID;

public interface TorreService {

    Torre save(UUID proyectoId, Torre torre);

    Torre findById(Long id);

    Torre findByIdWithPisos(Long id);

    List<Torre> findByProyectoId(UUID proyectoId);

    Torre update(Long id, Torre datos);

    void delete(Long id);
}
