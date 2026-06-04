package com.llosa.backend.proyecto.service;


import com.llosa.backend.proyecto.entity.Hito;

import java.util.List;
import java.util.UUID;


public interface HitoService {

    Hito save(UUID proyectoId, Hito hito);
    Hito save(Hito hito);
    Hito findById(UUID id);
    void deleteById(UUID id);
}
