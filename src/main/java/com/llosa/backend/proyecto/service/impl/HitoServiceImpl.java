package com.llosa.backend.proyecto.service.impl;


import com.llosa.backend.proyecto.entity.*;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.repository.EtapaRepository;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.HitoUnidadRepository;
import com.llosa.backend.proyecto.service.EtapaService;
import com.llosa.backend.proyecto.service.HitoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HitoServiceImpl implements HitoService {

    private final HitoRepository hitoRepository;
    private final EtapaService etapaService;
    private final ActivoRepository activoRepository;
    private final HidratationServiceImpl hidratacionService;

    @Override
    @Transactional
    public Hito save(Long etapaId, Hito hito) {
        Etapa etapa = etapaService.findById(etapaId);
        hito.setEtapa(etapa);
        Hito hitoGuardado = hitoRepository.save(hito);

        // Buscamos el Id del proyecto
        Proyecto proyecto = etapa.getProyecto();

        List<Activo> activosDelProyecto = activoRepository.findByPisoTorreProyectoId(proyecto.getId());
        hidratacionService.hidratarNuevoHito(hitoGuardado, activosDelProyecto);

        return hitoGuardado;
    }

}
