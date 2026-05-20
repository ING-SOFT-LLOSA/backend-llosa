package com.llosa.backend.proyecto.service;


import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.enums.EstadoHito;

import java.util.List;
import java.util.UUID;

public interface HitoService {

    Hito save(Long etapaId, Hito hito);

    Hito findById(UUID id);

    List<Hito> findByEtapaId(Long etapaId);

    List<Hito> findByEtapaIdAndEstado(Long etapaId, EstadoHito estado);

    Hito update(UUID id, Hito datos);

    Hito cambiarEstado(UUID id, EstadoHito nuevoEstado);

    void delete(UUID id);
}
