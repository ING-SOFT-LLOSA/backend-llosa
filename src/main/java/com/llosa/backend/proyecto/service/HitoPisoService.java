package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponsePorcentajeDTO;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.enums.EstadoHito;

import java.util.List;
import java.util.UUID;

public interface HitoPisoService {
    HitoPiso findById(UUID id);
    HitoPiso cambiarEstado(UUID id, EstadoHito nuevoEstado);
    List<HitoPiso> findByActivo(UUID activoId);
    List<AvanceUnidadResponsePorcentajeDTO> obtenerAvancesPorActivo(UUID activoId);
}
