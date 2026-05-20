package com.llosa.backend.proyecto.service;


import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.enums.EstadoEtapa;

import java.util.List;
import java.util.UUID;

public interface EtapaService {

    Etapa save(UUID proyectoId, Etapa etapa);

    Etapa findById(Long id);

    Etapa findByIdWithHitos(Long id);

    List<Etapa> findByProyectoId(UUID proyectoId);

    List<Etapa> findByProyectoIdAndEstado(UUID proyectoId, EstadoEtapa estado);

    Etapa update(Long id, Etapa datos);

    Etapa cambiarEstado(Long id, EstadoEtapa nuevoEstado);

    void delete(Long id);
}
