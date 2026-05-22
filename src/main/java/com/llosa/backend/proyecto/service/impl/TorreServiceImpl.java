package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.TorreRepository;
import com.llosa.backend.proyecto.service.TorreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TorreServiceImpl implements TorreService {
    private final TorreRepository torreRepository;
    private final ProyectoRepository proyectoRepository;

    public Torre save(UUID ProyectoId, Torre torre) {
        Proyecto proyecto = proyectoRepository.findById(ProyectoId).orElseThrow(
                () -> new RuntimeException("Proyecto no encontrado")
        );
        torre.setProyecto(proyecto);
        return torreRepository.save(torre);
    }
    public Torre findById(Long id){
        return torreRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Torre no encontrado")
        );
    }

}
