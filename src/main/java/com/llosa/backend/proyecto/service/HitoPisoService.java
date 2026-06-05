package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponseDTO;
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
    // CP18: compra tardía — marca en batch los hitos con orden < ordenActual sin disparar notificaciones
    void marcarHitosAnterioresCompletados(UUID activoId, int ordenActual);
    // CP23: actualización masiva por Torre completa
    List<HitoPiso> cambiarEstadoPorTorre(Long torreId, UUID hitoId, EstadoHito estado);
}
