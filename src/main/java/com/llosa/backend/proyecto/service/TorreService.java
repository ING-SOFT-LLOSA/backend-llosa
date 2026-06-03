package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Torre;


import java.util.UUID;

public interface TorreService {
    Torre save(UUID ProyectoId, Torre torre);
    Torre findById(Long id);
}
