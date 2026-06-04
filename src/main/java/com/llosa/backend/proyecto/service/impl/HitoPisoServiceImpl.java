package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponseDTO;
import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponsePorcentajeDTO;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.service.HitoPisoService;
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
public class HitoPisoServiceImpl implements HitoPisoService {

    private final HitoPisoRepository hitoPisoRepository;

    @Override
    @Transactional(readOnly = true)
    public HitoPiso findById(UUID id) {
        return hitoPisoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hito de unidad no encontrado: " + id));
    }

    @Override
    @Transactional
    public HitoPiso cambiarEstado(UUID id, EstadoHito nuevoEstado) {
        HitoPiso hitoPiso = findById(id);

        if (nuevoEstado == EstadoHito.COMPLETADO) {
            validarHitoAnterior(hitoPiso);
            hitoPiso.setFechaCompletado(LocalDate.now());
        } else {
            hitoPiso.setFechaCompletado(null);
        }

        hitoPiso.setEstado(nuevoEstado);
        return hitoPisoRepository.save(hitoPiso);
    }

    private void validarHitoAnterior(HitoPiso hitoPiso) {
        int ordenAnterior = hitoPiso.getHito().getOrden() - 1;
        if (ordenAnterior >= 1) { // Asumiendo que el orden empieza en 1
            HitoPiso anterior = hitoPisoRepository.findByPisoAndHito_Orden(hitoPiso.getPiso(), ordenAnterior)
                    .orElseThrow(() -> new BusinessException("No se encontró el hito anterior con orden " + ordenAnterior));

            if (anterior.getEstado() != EstadoHito.COMPLETADO) {
                throw new BusinessException("No se puede completar este hito porque el hito anterior (" +
                        anterior.getHito().getTitulo() + ") no está completado.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HitoPiso> findByActivo(UUID activoId) {
        return hitoPisoRepository.findByActivoIdOrderByHitoOrdenAsc(activoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvanceUnidadResponsePorcentajeDTO> obtenerAvancesPorActivo(UUID idActivo) {

        List<HitoPiso> hitos = hitoPisoRepository
                .findByActivoIdOrderByHitoOrdenAsc(idActivo);

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
