package com.llosa.backend.proyecto.service.impl;


import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.PisoService;
import com.llosa.backend.proyecto.entity.Activo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivoServiceImpl implements ActivoService {

    private final ActivoRepository activoRepository;
    private final PisoService pisoService;

    @Override
    @Transactional
    public Activo save(Long pisoId, Activo activo) {
        Piso piso = pisoService.findById(pisoId);
        activo.setPiso(piso);
        return activoRepository.save(activo);
    }

    @Override
    @Transactional(readOnly = true)
    public Activo findById(UUID id) {
        return activoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Activo no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activo> findByPisoId(Long pisoId) {
        return activoRepository.findByPiso_Id(pisoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activo> findByTorreId(Long torreId) {
        return activoRepository.findByTorreId(torreId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activo> findByProyectoId(UUID proyectoId) {
        return activoRepository.findByProyectoId(proyectoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activo> findByTipo(TipoActivo tipo) {
        return activoRepository.findByTipo(tipo);
    }

    @Override
    @Transactional
    public Activo update(UUID id, Activo datos) {
        Activo existente = findById(id);
        existente.setNro(datos.getNro());
        existente.setTipo(datos.getTipo());
        existente.setPrecio(datos.getPrecio());
        existente.setDescripcion(datos.getDescripcion());
        return activoRepository.save(existente);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!activoRepository.existsById(id)) {
            throw new EntityNotFoundException("Activo no encontrado: " + id);
        }
        activoRepository.deleteById(id);
    }
}
