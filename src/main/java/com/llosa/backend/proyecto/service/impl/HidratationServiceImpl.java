package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
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

    @Override
    @Transactional
    public void hidratarActivos(List<Activo> activos, UUID idProyecto){
        List<Hito> hitos = hitoRepository.findByProyectoId(idProyecto);
        if (hitos.isEmpty()) return;

        List<HitoPiso> nuevasJunturas = new ArrayList<>();

        List<Piso> pisosUnicos = activos.stream().map(Activo::getPiso).distinct().toList();

        for (Piso piso: pisosUnicos){
            for (Hito hito: hitos){
                HitoPiso hitoPiso = HitoPiso.builder()
                        .estado(EstadoHito.PENDIENTE)
                        .fechaCompletado(null)
                        .piso(piso)
                        .hito(hito)
                        .build();
                nuevasJunturas.add(hitoPiso);
            }
        }
        hitoPisoRepository.saveAll(nuevasJunturas);
    }

    @Override
    @Transactional
    public void hidratarNuevoHito(Hito nuevoHito, List<Activo> activosDelProyecto){
        List<Piso> pisosUnicos = activosDelProyecto.stream().map(Activo::getPiso).distinct().toList();
        List<HitoPiso> nuevasJunturas = pisosUnicos.stream()
                .map(piso -> HitoPiso.builder()
                        .piso(piso)
                        .hito(nuevoHito)
                        .estado(EstadoHito.PENDIENTE)
                        .build())
                .toList();
        hitoPisoRepository.saveAll(nuevasJunturas);
    }
}
