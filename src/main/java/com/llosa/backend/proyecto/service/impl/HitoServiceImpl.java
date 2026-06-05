package com.llosa.backend.proyecto.service.impl;


import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.entity.*;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.service.HidratationService;
import com.llosa.backend.proyecto.service.ProyectoService;
import com.llosa.backend.proyecto.service.HitoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HitoServiceImpl implements HitoService {

    private final HitoRepository hitoRepository;
    private final ProyectoService proyectoService;
    private final HidratationService hidratacionService;
    private final HitoPisoRepository hitoPisoRepository;

    @Override
    @Transactional
    public Hito save(UUID proyectoId, Hito hito) {
        Proyecto proyecto = proyectoService.findById(proyectoId);
        hito.setProyecto(proyecto);
        Hito hitoGuardado = hitoRepository.save(hito);

        hidratacionService.propagateMilestoneToProjectFloors(hitoGuardado, proyecto.getId());

        return hitoGuardado;
    }
    @Override
    @Transactional
    public Hito save(Hito hito) {
        return hitoRepository.save(hito);
    }

    @Override
    @Transactional
    public Hito findById(UUID id){
        return hitoRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Hito no encontrado")
        );
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        // CP14: hitos propagados a unidades son inmutables (estructura de seguimiento activa)
        if (hitoPisoRepository.countByHitoId(id) > 0) {
            throw new BusinessException("No se puede eliminar un hito que ya ha sido propagado a unidades del proyecto");
        }
        hitoRepository.deleteById(id);
    }
}

