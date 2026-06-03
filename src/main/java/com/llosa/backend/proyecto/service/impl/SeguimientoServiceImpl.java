package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.dto.response.SeguimientoResponseDTO;
import com.llosa.backend.proyecto.dto.shared.FaseActualDTO;
import com.llosa.backend.proyecto.dto.shared.PasoStepperDTO;
import com.llosa.backend.proyecto.entity.HitoUnidad;
import com.llosa.backend.proyecto.repository.HitoUnidadRepository;
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

    // Solo inyectamos el repositorio técnico, ¡cero dependencias comerciales!
    private final HitoUnidadRepository hitoUnidadRepository;

    @Override
    public SeguimientoResponseDTO obtenerSeguimiento(UUID idActivo) {

        // 1. Traemos todos los hitos de la unidad ordenados de inicio a fin
        List<HitoUnidad> hitos = hitoUnidadRepository.findByActivo_IdOrderByHito_OrdenAsc(idActivo);

        if (hitos == null || hitos.isEmpty()) {
            throw new RuntimeException("No se encontraron hitos de obra para este activo.");
        }

        List<PasoStepperDTO> stepper = new ArrayList<>();
        HitoUnidad hitoActualEnProgreso = null;
        boolean hitoActualEncontrado = false;

        // 2. Armar el Stepper y descubrir cuál es la fase actual
        for (HitoUnidad hu : hitos) {
            String estadoVisual;

            if ("COMPLETADO".equalsIgnoreCase(hu.getEstado().name())) {
                estadoVisual = "COMPLETADO";
            } else if (!hitoActualEncontrado) {
                // El primer hito NO completado es el que está en progreso ahora mismo
                estadoVisual = "EN_PROGRESO";
                hitoActualEnProgreso = hu;
                hitoActualEncontrado = true;
            } else {
                // Todos los demás hacia adelante están pendientes
                estadoVisual = "PENDIENTE";
            }

            stepper.add(PasoStepperDTO.builder()
                    .nombre(hu.getHito().getTitulo()) // El título del hito base
                    .estado(estadoVisual)
                    .orden(hu.getHito().getOrden())
                    .build());
        }

        // 3. Armar el objeto FaseActualDTO
        FaseActualDTO faseActualDTO;

        if (hitoActualEnProgreso != null) {
            // Calcular el porcentaje de la etapa a la que pertenece este hito
            Long idEtapaActual = hitoActualEnProgreso.getHito().getEtapa().getId();

            List<HitoUnidad> hitosDeEstaEtapa = hitos.stream()
                    .filter(h -> h.getHito().getEtapa().getId().equals(idEtapaActual))
                    .toList();

            long completadosEnEtapa = hitosDeEstaEtapa.stream()
                    .filter(h -> "COMPLETADO".equalsIgnoreCase(h.getEstado().name()))
                    .count();

            double porcentaje = hitosDeEstaEtapa.isEmpty() ? 0.0 :
                    ((double) completadosEnEtapa / hitosDeEstaEtapa.size()) * 100.0;

            faseActualDTO = FaseActualDTO.builder()
                    .uuidHitoU(hitoActualEnProgreso.getId())
                    .titulo(hitoActualEnProgreso.getHito().getTitulo())
                    // Usamos la descripción de la etapa, ya que el Hito no tiene campo descripción
                    .descripcion(hitoActualEnProgreso.getHito().getEtapa().getDescripcion())
                    .porcentajeEtapa(Math.round(porcentaje * 100.0) / 100.0) // Redondear a 2 decimales
                    .fechaInicioFase(hitoActualEnProgreso.getUpdatedAt() != null ? 
                            hitoActualEnProgreso.getUpdatedAt() : LocalDateTime.now())
                    .build();

        } else {
            // Si hitoActualEnProgreso es NULL, significa que TODOS están completados
            HitoUnidad ultimoHito = hitos.getLast();

            faseActualDTO = FaseActualDTO.builder()
                    .uuidHitoU(ultimoHito.getId())
                    .titulo("Obra Finalizada")
                    .descripcion("El proyecto ha completado todas sus fases de construcción al 100%.")
                    .porcentajeEtapa(100.0)
                    .fechaInicioFase(ultimoHito.getFechaCompletado() != null ?
                            ultimoHito.getFechaCompletado().atStartOfDay() : 
                            (ultimoHito.getUpdatedAt() != null ? ultimoHito.getUpdatedAt() : LocalDateTime.now()))
                    .build();
        }

        // 4. Retornar el DTO intacto para el Frontend
        return SeguimientoResponseDTO.builder()
                .stepper(stepper)
                .faseActual(faseActualDTO)
                .build();
    }
}