package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.repository.PisoRepository;
import com.llosa.backend.proyecto.service.HidratationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HidratationServiceImpl implements HidratationService {

    private final HitoRepository hitoRepository;
    private final HitoPisoRepository hitoPisoRepository;
    private final PisoRepository pisoRepository;

    @Override
    @Transactional
    public void hydrateFloorMilestones(Long idPiso) {
        Piso piso = pisoRepository.findById(idPiso)
                .orElseThrow(() -> new RuntimeException("Piso no encontrado"));
        
        UUID idProyecto = piso.getTorre().getProyecto().getId();
        List<Hito> hitos = hitoRepository.findByProyectoId(idProyecto);
        
        if (hitos.isEmpty()) return;

        List<HitoPiso> nuevasJunturas = new ArrayList<>();
        for (Hito hito : hitos) {
            if (hitoPisoRepository.existsByPisoIdAndHitoIdFalse(idPiso, hito.getId())) {
                HitoPiso hitoPiso = HitoPiso.builder()
                        .estado(EstadoHito.PENDIENTE)
                        .fechaCompletado(null)
                        .piso(piso)
                        .hito(hito)
                        .build();
                nuevasJunturas.add(hitoPiso);
            }
        }
        
        if (!nuevasJunturas.isEmpty()) {
            hitoPisoRepository.saveAll(nuevasJunturas);
        }
    }


    @Override
    @Transactional
    public void propagateMilestoneToProjectFloors(Hito nuevoHito, UUID idProyecto) {
        List<Piso> todosLosPisos = pisoRepository.findByTorreProyectoId(idProyecto);
        List<HitoPiso> nuevasJunturas = new ArrayList<>();
        
        for (Piso piso : todosLosPisos) {
            if (hitoPisoRepository.existsByPisoIdAndHitoIdFalse(piso.getId(), nuevoHito.getId())) {
                nuevasJunturas.add(HitoPiso.builder()
                        .piso(piso)
                        .hito(nuevoHito)
                        .estado(EstadoHito.PENDIENTE)
                        .build());
            }
        }
        
        if (!nuevasJunturas.isEmpty()) {
            hitoPisoRepository.saveAll(nuevasJunturas);
        }
    }
}
