package com.llosa.backend.proyecto.service.impl;


import com.llosa.backend.proyecto.entity.*;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.service.EtapaService;
import com.llosa.backend.proyecto.service.HitoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

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
    public void deleteById(UUID id){
        hitoRepository.deleteById(id);
        return;
    }
}
