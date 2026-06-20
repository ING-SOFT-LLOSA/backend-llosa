package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.exception.BusinessException;
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
            throw new BusinessException("No se encontraron hitos de obra para el piso de este activo.");
        }

        List<PasoStepperDTO> stepper = new ArrayList<>();
        HitoPiso hitoActualEnProgreso = null;
        boolean hitoActualEncontrado = false;

        // 2. Map milestones to visual states
        for (HitoPiso hp : hitos) {
            String estadoVisual;

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

        // 3. Build the response DTO delegando la lógica
        FaseActualDTO faseActualDTO = construirFaseActual(hitos, hitoActualEnProgreso);

        return SeguimientoResponseDTO.builder()
                .stepper(stepper)
                .faseActual(faseActualDTO)
                .build();
    }
    private FaseActualDTO construirFaseActual(List<HitoPiso> hitos, HitoPiso hitoActualEnProgreso) {
        if (hitoActualEnProgreso != null) {
            return construirFaseEnProgreso(hitos, hitoActualEnProgreso);
        }
        return construirFaseFinalizada(hitos.getLast());
    }

    private FaseActualDTO construirFaseEnProgreso(List<HitoPiso> hitos, HitoPiso hitoActualEnProgreso) {
        long completadosTotales = hitos.stream()
                .filter(h -> h.getEstado() == EstadoHito.COMPLETADO)
                .count();

        double porcentaje = ((double) completadosTotales / hitos.size()) * 100.0;

        return FaseActualDTO.builder()
                .uuidHitoU(hitoActualEnProgreso.getId())
                .titulo(hitoActualEnProgreso.getHito().getTitulo())
                .descripcion("Fase en progreso: " + hitoActualEnProgreso.getHito().getTitulo())
                .porcentajeEtapa(Math.round(porcentaje * 100.0) / 100.0)
                .fechaInicioFase(hitoActualEnProgreso.getUpdatedAt() != null
                        ? hitoActualEnProgreso.getUpdatedAt()
                        : LocalDateTime.now())
                .build();
    }

    private FaseActualDTO construirFaseFinalizada(HitoPiso ultimoHito) {
        return FaseActualDTO.builder()
                .uuidHitoU(ultimoHito.getId())
                .titulo("Obra Finalizada")
                .descripcion("El piso ha completado todas sus fases de construcción al 100%.")
                .porcentajeEtapa(100.0)
                .fechaInicioFase(determinarFechaFinalizada(ultimoHito))
                .build();
    }

    private LocalDateTime determinarFechaFinalizada(HitoPiso ultimoHito) {
        if (ultimoHito.getFechaCompletado() != null) {
            return ultimoHito.getFechaCompletado().atStartOfDay();
        }
        if (ultimoHito.getUpdatedAt() != null) {
            return ultimoHito.getUpdatedAt();
        }
        return LocalDateTime.now();
    }
}