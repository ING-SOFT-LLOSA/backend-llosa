package com.llosa.backend.proyecto.service;


import com.llosa.backend.proyecto.entity.Hito;

import java.util.List;
import java.util.UUID;


public interface HitoService {

    Hito save(Long etapaId, Hito hito);
}
