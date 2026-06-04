package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.repository.PisoRepository;
import com.llosa.backend.proyecto.service.PisoService;
import com.llosa.backend.proyecto.service.TorreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PisoServiceImpl implements PisoService {

    private final PisoRepository pisoRepository;
    private final TorreService torreService;

    @Override
    @Transactional(readOnly = true)
    public Piso findById(Long id){
        return pisoRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Piso no encontrado")
        );
    }

    @Override
    @Transactional
    public Piso save(Long torreId, Piso piso){
        Torre torre = torreService.findById(torreId);
        piso.setTorre(torre);
        return pisoRepository.save(piso);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Piso> findByTorre(Long torreId, String search) {
        if (search == null || search.isBlank()) {
            return pisoRepository.findByTorreId(torreId);
        }
        return pisoRepository.findByTorreIdAndSearch(torreId, search);
    }
}
