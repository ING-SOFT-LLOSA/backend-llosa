package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.repository.TorreRepository;
import com.llosa.backend.proyecto.service.ProyectoService;
import com.llosa.backend.proyecto.service.TorreService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TorreServiceImpl implements TorreService {

    private final TorreRepository torreRepository;
    private final ProyectoService proyectoService;

    @Override
    @Transactional
    public Torre save(UUID proyectoId, Torre torre) {
        Proyecto proyecto = proyectoService.findById(proyectoId);
        torre.setProyecto(proyecto);
        return torreRepository.save(torre);
    }

    @Override
    @Transactional(readOnly = true)
    public Torre findById(Long id) {
        return torreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Torre no encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Torre findByIdWithPisos(Long id) {
        return torreRepository.findByIdWithPisos(id)
                .orElseThrow(() -> new EntityNotFoundException("Torre no encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Torre> findByProyectoId(UUID proyectoId) {
        return torreRepository.findByProyecto_Id(proyectoId);
    }

    @Override
    @Transactional
    public Torre update(Long id, Torre datos) {
        Torre existente = findById(id);
        existente.setNombre(datos.getNombre());
        return torreRepository.save(existente);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!torreRepository.existsById(id)) {
            throw new EntityNotFoundException("Torre no encontrada: " + id);
        }
        torreRepository.deleteById(id);
    }
}
