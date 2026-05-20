package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.repository.PisoRepository;
import com.llosa.backend.proyecto.service.PisoService;
import com.llosa.backend.proyecto.service.TorreService;
import jakarta.persistence.EntityNotFoundException;
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
    @Transactional
    public Piso save(Long torreId, Piso piso) {
        Torre torre = torreService.findById(torreId);
        piso.setTorre(torre);
        return pisoRepository.save(piso);
    }

    @Override
    @Transactional(readOnly = true)
    public Piso findById(Long id) {
        return pisoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Piso no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Piso> findByTorreId(Long torreId) {
        return pisoRepository.findByTorre_Id(torreId);
    }

    @Override
    @Transactional
    public Piso update(Long id, Piso datos) {
        Piso existente = findById(id);
        existente.setNroPiso(datos.getNroPiso());
        return pisoRepository.save(existente);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!pisoRepository.existsById(id)) {
            throw new EntityNotFoundException("Piso no encontrado: " + id);
        }
        pisoRepository.deleteById(id);
    }
}
