package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Hito;
import java.util.UUID;

public interface HidratationService {
    void propagateMilestoneToProjectFloors(Hito nuevoHito, UUID idProyecto);
    void hydrateFloorMilestones(Long idPiso);
}
