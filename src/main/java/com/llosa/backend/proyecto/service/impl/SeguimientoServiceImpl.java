package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.dto.response.SeguimientoResponseDTO;
import com.llosa.backend.proyecto.dto.shared.FaseActualDTO;
import com.llosa.backend.proyecto.dto.shared.PasoStepperDTO;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.service.SeguimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeguimientoServiceImpl implements SeguimientoService {

    private final HitoPisoRepository hitoPisoRepository;

    @Override
    @Transactional(readOnly = true)
    public SeguimientoResponseDTO obtenerSeguimiento(UUID idActivo) {

        // 1. Fetch the milestones assigned to the floor of this active asset
        List<HitoPiso> hitos = hitoPisoRepository.findByActivoIdOrderByHitoOrdenAsc(idActivo);

        if (hitos == null || hitos.isEmpty()) {
            throw new RuntimeException("No se encontraron hitos de obra para el piso de este activo.");
        }

        List<PasoStepperDTO> stepper = new ArrayList<>();
        HitoPiso hitoActualEnProgreso = null;
        boolean hitoActualEncontrado = false;

        // 2. Map milestones to visual states
        for (HitoPiso hp : hitos) {
            String estadoVisual;

            // Direct Enum comparison (Safer and faster than .name().equalsIgnoreCase)
            if (hp.getEstado() == EstadoHito.COMPLETADO) {
                estadoVisual = "COMPLETADO";
            } else if (!hitoActualEncontrado) {
                estadoVisual = "EN_PROGRESO";
                hitoActualEnProgreso = hp;
                hitoActualEncontrado = true;
            } else {
                estadoVisual = "PENDIENTE";
            }

            stepper.add(PasoStepperDTO.builder()
                    .nombre(hp.getHito().getTitulo())
                    .estado(estadoVisual)
                    .orden(hp.getHito().getOrden())
                    .build());
        }

        // 3. Build the response DTO
        FaseActualDTO faseActualDTO;

        if (hitoActualEnProgreso != null) {
            long completadosTotales = hitos.stream()
                    .filter(h -> h.getEstado() == EstadoHito.COMPLETADO)
                    .count();

            double porcentaje = ((double) completadosTotales / hitos.size()) * 100.0;

            faseActualDTO = FaseActualDTO.builder()
                    .uuidHitoU(hitoActualEnProgreso.getId())
                    .titulo(hitoActualEnProgreso.getHito().getTitulo())
                    .descripcion("Fase en progreso: " + hitoActualEnProgreso.getHito().getTitulo())
                    .porcentajeEtapa(Math.round(porcentaje * 100.0) / 100.0) // Redondeo seguro a 2 decimales
                    .fechaInicioFase(hitoActualEnProgreso.getUpdatedAt() != null ?
                            hitoActualEnProgreso.getUpdatedAt() : LocalDateTime.now())
                    .build();

        } else {
            // If all milestones are completed, return 100% complete
            HitoPiso ultimoHito = hitos.getLast();

            faseActualDTO = FaseActualDTO.builder()
                    .uuidHitoU(ultimoHito.getId())
                    .titulo("Obra Finalizada")
                    .descripcion("El piso ha completado todas sus fases de construcción al 100%.")
                    .porcentajeEtapa(100.0)
                    .fechaInicioFase(ultimoHito.getFechaCompletado() != null ?
                            ultimoHito.getFechaCompletado().atStartOfDay() :
                            (ultimoHito.getUpdatedAt() != null ? ultimoHito.getUpdatedAt() : LocalDateTime.now()))
                    .build();
        }

        return SeguimientoResponseDTO.builder()
                .stepper(stepper)
                .faseActual(faseActualDTO)
                .build();
    }
}