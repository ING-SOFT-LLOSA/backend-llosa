package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.service.EtapaService;
import com.llosa.backend.proyecto.service.HitoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HitoServiceImpl implements HitoService {

    private final HitoRepository hitoRepository;
    private final EtapaService etapaService;

    @Override
    @Transactional
    public Hito save(Long etapaId, Hito hito) {
        Etapa etapa = etapaService.findById(etapaId);
        hito.setEtapa(etapa);
        return hitoRepository.save(hito);
    }

}
