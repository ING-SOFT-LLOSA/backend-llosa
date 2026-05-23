package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.repository.EtapaRepository;
import com.llosa.backend.proyecto.service.EtapaService;
import com.llosa.backend.proyecto.service.ProyectoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

}
