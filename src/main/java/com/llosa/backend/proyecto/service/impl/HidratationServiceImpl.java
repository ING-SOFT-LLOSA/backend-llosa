package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.HitoUnidad;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.HitoUnidadRepository;
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
    private final HitoUnidadRepository hitoUnidadRepository;

    @Override
    @Transactional
    public void hidratarActivos(List<Activo> activos, UUID idProyecto){
        List<Hito> hitos = hitoRepository.findByEtapaProyectoId(idProyecto);
        if (hitos.isEmpty()) return;

        List<HitoUnidad> nuevasJunturas = new ArrayList<>();

        for (Activo activo: activos){
            for (Hito hito: hitos){
                HitoUnidad hitoUnidad = HitoUnidad.builder()
                        .estado(EstadoHito.PENDIENTE)
                        .fechaCompletado(null)
                        .activo(activo)
                        .hito(hito)
                        .build();
                nuevasJunturas.add(hitoUnidad);
            }
        }
        hitoUnidadRepository.saveAll(nuevasJunturas);
    }

    @Override
    @Transactional
    public void hidratarNuevoHito(Hito nuevoHito, List<Activo> activosDelProyecto){
        List<HitoUnidad> nuevasJunturas = activosDelProyecto.stream()
                .map(activo -> HitoUnidad.builder()
                        .activo(activo)
                        .hito(nuevoHito)
                        .estado(EstadoHito.PENDIENTE)
                        .build())
                .toList();
        hitoUnidadRepository.saveAll(nuevasJunturas);
    }
}
