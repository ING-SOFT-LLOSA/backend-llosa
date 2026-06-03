package com.llosa.backend.proyecto.service;


import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.entity.Proyecto;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface EtapaService {

    Etapa save(UUID proyectoId, Etapa etapa);

    Etapa findById(Long id);

    void deleteById(Long id);
}
