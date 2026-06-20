package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.factory.FlujoConstruccionFactory;
import com.llosa.backend.proyecto.dto.request.*;
import com.llosa.backend.proyecto.entity.*;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.service.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProyectoServiceImpl implements ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final HitoPisoRepository hitoPisoRepository;
    private final TorreService torreService;
    private final ActivoService activoService;
    private final PisoService pisoService;
    private final FlujoConstruccionFactory flujoConstruccionFactory;

    @Override
    public Proyecto save(Proyecto proyecto) {
        boolean esNuevoProyecto = proyecto.getId() == null;
        if (esNuevoProyecto) {
            List<Hito> hitosPorDefecto = flujoConstruccionFactory.generarHitosPorDefecto();
            hitosPorDefecto.forEach(hito -> hito.setProyecto(proyecto));
            proyecto.setHitos(hitosPorDefecto);
        }
        return proyectoRepository.save(proyecto);
    }

    @Override
    @Transactional(readOnly = true)
    public Proyecto findById(UUID id) {
        return proyectoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proyecto> findAll(String search) {
        if (search == null || search.isBlank()) {
            return proyectoRepository.findAll();
        }
        return proyectoRepository.findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(search, search);
    }

    @Override
    @Transactional(readOnly = true)
    public double getPorcentajeAvance(UUID id) {
        long totales = hitoPisoRepository.countByProyectoId(id);
        if (totales == 0) return 0.0;
        long completados = hitoPisoRepository.countByProyectoIdAndEstado(id, EstadoHito.COMPLETADO);
        return (double) completados * 100 / totales;
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        proyectoRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void cargarProyecto(UUID idProyecto, ProyectoCargaDTO dto) {
        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));


        for (TorreRequestDTO torreRequestDTO : dto.torres()){
            Torre torre = Torre.builder()
                    .nombre(torreRequestDTO.nombre())
                    .build();
            Torre torreGuardada = torreService.save(idProyecto, torre);
            for (PisoRequestDTO pisoRequestDTO : torreRequestDTO.pisos()){
                Piso piso = Piso.builder()
                        .nroPiso(pisoRequestDTO.nroPiso())
                        .build();
                Piso pisoGuardado = pisoService.save(torreGuardada.getId(), piso);
                for (ActivoRequestDTO activoRequestDTO : pisoRequestDTO.activos()){
                    Activo activo = Activo.builder()
                            .nro(activoRequestDTO.nro())
                            .tipo(activoRequestDTO.tipo())
                            .areaM2(activoRequestDTO.areaM2())
                            .estadoComercial(activoRequestDTO.estadoComercial())
                            .precio(activoRequestDTO.precio())
                            .descripcion(activoRequestDTO.descripcion())
                            .build();
                    Activo activoGuardado = activoService.saveFisico(pisoGuardado.getId(), activo);
                }
            }
        }
        proyectoRepository.save(proyecto);
    }

    @Override
    @Transactional
    public List<Hito> findHitosByProyecto(UUID idProyecto) {
        Proyecto proyecto = findById(idProyecto);
        List<Hito> hitos = proyecto.getHitos();

        for (Hito hito : hitos) {
            // Solo procesamos si no está COMPLETADO para evitar redundancia
            if (hito.getEstado() != EstadoHito.COMPLETADO) {
                List<HitoPiso> hitosPiso = hito.getHitosPiso();
                // Verificamos si todos los hitos de pisos están completados
                if (!hitosPiso.isEmpty() && hitosPiso.stream().allMatch(hp -> hp.getEstado() == EstadoHito.COMPLETADO)) {
                    hito.setEstado(EstadoHito.COMPLETADO);
                    hito.setFechaCompletado(LocalDate.now());
                }
            }
        }
        // No es necesario llamar explícitamente a save si los objetos están gestionados por JPA, 
        // pero se retornan los hitos actualizados.
        return hitos;
    }
}
