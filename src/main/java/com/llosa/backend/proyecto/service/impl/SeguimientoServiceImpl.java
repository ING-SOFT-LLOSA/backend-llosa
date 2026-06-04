package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.dto.response.SeguimientoResponseDTO;
import com.llosa.backend.proyecto.dto.shared.FaseActualDTO;
import com.llosa.backend.proyecto.dto.shared.PasoStepperDTO;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.service.SeguimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeguimientoServiceImpl implements SeguimientoService {

    private final HitoPisoRepository hitoPisoRepository;

    @Override
    public SeguimientoResponseDTO obtenerSeguimiento(UUID idActivo) {

        List<HitoPiso> hitos = hitoPisoRepository.findByActivoIdOrderByHitoOrdenAsc(idActivo);

        if (hitos == null || hitos.isEmpty()) {
            throw new RuntimeException("No se encontraron hitos de obra para el piso de este activo.");
        }

        List<PasoStepperDTO> stepper = new ArrayList<>();
        HitoPiso hitoActualEnProgreso = null;
        boolean hitoActualEncontrado = false;

        for (HitoPiso hp : hitos) {
            String estadoVisual;

            if ("COMPLETADO".equalsIgnoreCase(hp.getEstado().name())) {
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

        FaseActualDTO faseActualDTO;

        if (hitoActualEnProgreso != null) {
            long completadosTotales = hitos.stream()
                    .filter(h -> "COMPLETADO".equalsIgnoreCase(h.getEstado().name()))
                    .count();

            double porcentaje = hitos.isEmpty() ? 0.0 :
                    ((double) completadosTotales / hitos.size()) * 100.0;

            faseActualDTO = FaseActualDTO.builder()
                    .uuidHitoU(hitoActualEnProgreso.getId())
                    .titulo(hitoActualEnProgreso.getHito().getTitulo())
                    .descripcion("Fase en progreso: " + hitoActualEnProgreso.getHito().getTitulo())
                    .porcentajeEtapa(Math.round(porcentaje * 100.0) / 100.0)
                    .fechaInicioFase(hitoActualEnProgreso.getUpdatedAt() != null ? 
                            hitoActualEnProgreso.getUpdatedAt() : LocalDateTime.now())
                    .build();

        } else {
            HitoPiso ultimoHito = hitos.get(hitos.size() - 1);

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