package com.llosa.backend.proyecto.service;


import com.llosa.backend.proyecto.entity.Etapa;
import java.util.UUID;

public interface EtapaService {

    Etapa save(UUID proyectoId, Etapa etapa);

    Etapa findById(Long id);
}
