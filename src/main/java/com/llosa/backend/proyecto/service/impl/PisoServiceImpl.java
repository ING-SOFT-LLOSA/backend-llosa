package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.repository.PisoRespository;
import com.llosa.backend.proyecto.service.PisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PisoServiceImpl implements PisoService {

    private final PisoRespository pisoRepository;
    private final TorreServiceImpl torreService;

    @Override
    public Piso findById(Long id){
        return pisoRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Piso no encontrado")
        );
    }
    @Override
    public Piso save(Long torreId, Piso piso){
        Torre torre = torreService.findById(torreId);
        piso.setTorre(torre);
        return pisoRepository.save(piso);
    }
}
