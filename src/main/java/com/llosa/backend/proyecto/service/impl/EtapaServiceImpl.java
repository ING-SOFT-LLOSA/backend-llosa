package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.enums.EstadoEtapa;
import com.llosa.backend.proyecto.repository.EtapaRepository;
import com.llosa.backend.proyecto.service.EtapaService;
import com.llosa.backend.proyecto.service.ProyectoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EtapaServiceImpl implements EtapaService {

    private final EtapaRepository etapaRepository;
    private final ProyectoService proyectoService;

    @Override
    @Transactional
    public Etapa save(UUID proyectoId, Etapa etapa) {
        Proyecto proyecto = proyectoService.findById(proyectoId);
        etapa.setProyecto(proyecto);
        return etapaRepository.save(etapa);
    }

    @Override
    @Transactional(readOnly = true)
    public Etapa findById(Long id) {
        return etapaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Etapa no encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Etapa findByIdWithHitos(Long id) {
        return etapaRepository.findByIdWithHitos(id)
                .orElseThrow(() -> new EntityNotFoundException("Etapa no encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Etapa> findByProyectoId(UUID proyectoId) {
        return etapaRepository.findByProyecto_Id(proyectoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Etapa> findByProyectoIdAndEstado(UUID proyectoId, EstadoEtapa estado) {
        return etapaRepository.findByProyecto_IdAndEstado(proyectoId, estado);
    }

    @Override
    @Transactional
    public Etapa update(Long id, Etapa datos) {
        Etapa existente = findById(id);
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setEstado(datos.getEstado());
        return etapaRepository.save(existente);
    }

    @Override
    @Transactional
    public Etapa cambiarEstado(Long id, EstadoEtapa nuevoEstado) {
        Etapa etapa = findById(id);
        etapa.setEstado(nuevoEstado);
        return etapaRepository.save(etapa);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!etapaRepository.existsById(id)) {
            throw new EntityNotFoundException("Etapa no encontrada: " + id);
        }
        etapaRepository.deleteById(id);
    }
}
