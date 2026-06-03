package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Activo;

import java.util.UUID;

public interface ActivoService {
    Activo saveFisico(Long pisoId, Activo activo);
    Activo saveIndividual(Long pisoId, Activo activo);
    Activo save(Activo activo);
    Activo findById(UUID id);
    void deleteById(UUID id);
}
