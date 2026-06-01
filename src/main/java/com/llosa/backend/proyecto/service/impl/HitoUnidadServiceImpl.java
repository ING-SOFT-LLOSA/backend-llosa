package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponseDTO;
import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponsePorcentajeDTO;
import com.llosa.backend.proyecto.entity.HitoUnidad;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoUnidadRepository;
import com.llosa.backend.proyecto.service.HitoUnidadService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HitoUnidadServiceImpl implements HitoUnidadService {

    private final HitoUnidadRepository hitoUnidadRepository;

    @Override
    @Transactional(readOnly = true)
    public HitoUnidad findById(UUID id) {
        return hitoUnidadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hito de unidad no encontrado: " + id));
    }

    @Override
    @Transactional
    public HitoUnidad cambiarEstado(UUID id, EstadoHito nuevoEstado) {
        HitoUnidad hitoUnidad = findById(id);

        if (nuevoEstado == EstadoHito.COMPLETADO) {
            validarHitoAnterior(hitoUnidad);
            hitoUnidad.setFechaCompletado(LocalDate.now());
        } else {
            hitoUnidad.setFechaCompletado(null);
        }

        hitoUnidad.setEstado(nuevoEstado);
        return hitoUnidadRepository.save(hitoUnidad);
    }

    private void validarHitoAnterior(HitoUnidad hitoUnidad) {
        int ordenAnterior = hitoUnidad.getHito().getOrden() - 1;
        if (ordenAnterior >= 1) { // Asumiendo que el orden empieza en 1
            HitoUnidad anterior = hitoUnidadRepository.findByActivoAndHito_Orden(hitoUnidad.getActivo(), ordenAnterior)
                    .orElseThrow(() -> new BusinessException("No se encontró el hito anterior con orden " + ordenAnterior));

            if (anterior.getEstado() != EstadoHito.COMPLETADO) {
                throw new BusinessException("No se puede completar este hito porque el hito anterior (" +
                        anterior.getHito().getTitulo() + ") no está completado.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HitoUnidad> findByActivo(UUID activoId) {
        return hitoUnidadRepository.findByActivo_IdOrderByHito_OrdenAsc(activoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvanceUnidadResponsePorcentajeDTO> obtenerAvancesPorActivo(UUID idActivo) {

        List<HitoUnidad> hitos = hitoUnidadRepository
                .findByActivo_IdOrderByHito_OrdenAsc(idActivo);

        if (hitos.isEmpty()) {
            return List.of();
        }

        int totalHitos = hitos.size();

        int completados = (int) hitos.stream()
                .filter(hu -> hu.getEstado() == EstadoHito.COMPLETADO)
                .count();

        int porcentaje = (completados * 100) / totalHitos;

        return hitos.stream()
                .map(hu -> AvanceUnidadResponsePorcentajeDTO.fromEntity(
                        hu,
                        porcentaje
                ))
                .toList();
    }

}
