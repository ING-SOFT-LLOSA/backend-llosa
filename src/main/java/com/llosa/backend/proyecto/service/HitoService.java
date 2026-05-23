package com.llosa.backend.proyecto.service;


import com.llosa.backend.proyecto.entity.Hito;


public interface HitoService {

    Hito save(Long etapaId, Hito hito);
}
