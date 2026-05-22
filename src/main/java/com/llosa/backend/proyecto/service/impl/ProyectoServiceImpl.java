package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.dto.request.*;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoUnidadRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.service.PisoService;
import com.llosa.backend.proyecto.service.ProyectoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProyectoServiceImpl implements ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final HitoUnidadRepository hitoUnidadRepository;
    private final TorreServiceImpl torreService;
    private final ActivoServiceImpl activoService;
    private final PisoService pisoService;

    @Override
    public Proyecto save(Proyecto proyecto) {
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
    public List<Proyecto> findAll() {
        return proyectoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public double getPorcentajeAvance(UUID id) {
        long totales = hitoUnidadRepository.countByProyectoId(id);
        if (totales == 0) return 0.0;
        long completados = hitoUnidadRepository.countByProyectoIdAndEstado(id, EstadoHito.COMPLETADO);
        return (double) completados * 100 / totales;
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
                    activoService.save(pisoGuardado.getId(), activo);
                }
            }
        }
        proyectoRepository.save(proyecto);
    }
}
