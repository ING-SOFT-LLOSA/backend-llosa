package com.llosa.backend.proyecto.service;



import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.enums.TipoActivo;

import java.util.List;
import java.util.UUID;

public interface ActivoService {

    Activo save(Long pisoId, Activo activo);

    Activo findById(UUID id);

    List<Activo> findByPisoId(Long pisoId);

    List<Activo> findByTorreId(Long torreId);

    List<Activo> findByProyectoId(UUID proyectoId);

    List<Activo> findByTipo(TipoActivo tipo);

    Activo update(UUID id, Activo datos);

    void delete(UUID id);
}
