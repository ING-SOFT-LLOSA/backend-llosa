package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
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

    @Override
    @Transactional
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
    public Proyecto findByIdWithTorresAndPisos(UUID id) {
        return proyectoRepository.findByIdWithTorresAndPisos(id)
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Proyecto findArbolFisico(UUID id) {
        return proyectoRepository.findByIdWithArbolFisico(id)
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Proyecto findCronograma(UUID id) {
        return proyectoRepository.findByIdWithCronograma(id)
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proyecto> findAll() {
        return proyectoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proyecto> findByDistrito(String distrito) {
        return proyectoRepository.findByDistrito(distrito);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proyecto> findByNombre(String nombre) {
        return proyectoRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Override
    @Transactional
    public Proyecto update(UUID id, Proyecto datos) {
        Proyecto existente = findById(id);
        existente.setNombre(datos.getNombre());
        existente.setDistrito(datos.getDistrito());
        existente.setDireccion(datos.getDireccion());
        existente.setFechaInicio(datos.getFechaInicio());
        return proyectoRepository.save(existente);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!proyectoRepository.existsById(id)) {
            throw new EntityNotFoundException("Proyecto no encontrado: " + id);
        }
        proyectoRepository.deleteById(id);
    }
}
