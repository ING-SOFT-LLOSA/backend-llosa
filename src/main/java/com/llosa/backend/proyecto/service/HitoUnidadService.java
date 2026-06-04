package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.HitoUnidad;
import com.llosa.backend.proyecto.enums.EstadoHito;

import java.util.List;
import java.util.UUID;

public interface HitoUnidadService {
    HitoUnidad findById(UUID id);
    HitoUnidad cambiarEstado(UUID id, EstadoHito nuevoEstado);
    List<HitoUnidad> findByActivo(UUID activoId);
}
