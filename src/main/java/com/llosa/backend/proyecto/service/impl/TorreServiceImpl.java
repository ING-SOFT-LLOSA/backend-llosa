package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.repository.TorreRepository;
import com.llosa.backend.proyecto.service.TorreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TorreServiceImpl implements TorreService {
    private final TorreRepository torreRepository;
    private final ProyectoServiceImpl proyectoService;

    public Torre save(UUID ProyectoId, Torre torre) {
        Proyecto proyecto = proyectoService.findById(ProyectoId);
        torre.setProyecto(proyecto);
        return torreRepository.save(torre);
    }
    public Torre findById(Long id){
        return torreRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Torre no encontrado")
        );
    }

}
