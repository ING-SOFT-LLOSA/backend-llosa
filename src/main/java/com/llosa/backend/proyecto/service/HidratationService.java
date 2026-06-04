package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Hito;

import java.util.List;
import java.util.UUID;

public interface HidratationService {
    void hidratarActivos(List<Activo> activos, UUID idProyecto);
    void propagateMilestoneToProjectFloors(Hito nuevoHito, UUID idProyecto);
    void hydrateFloorMilestones(Long idPiso);
}
