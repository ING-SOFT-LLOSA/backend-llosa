package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.repository.ActivoRepository;

import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.PisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivoServiceImpl implements ActivoService {

    private final ActivoRepository activoRepository;
    private final PisoService pisoService;

    @Override
    @Transactional
    public Activo save(Long pisoId, Activo activo){
        Piso piso = pisoService.findById(pisoId);
        activo.setPiso(piso);
        return activoRepository.save(activo);
    }
}
