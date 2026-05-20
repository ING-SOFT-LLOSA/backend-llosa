package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.service.EtapaService;
import com.llosa.backend.proyecto.service.HitoService;
import jakarta.persistence.EntityNotFoundException;
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

    @Override
    @Transactional
    public Hito save(Long etapaId, Hito hito) {
        Etapa etapa = etapaService.findById(etapaId);
        hito.setEtapa(etapa);
        return hitoRepository.save(hito);
    }

    @Override
    @Transactional(readOnly = true)
    public Hito findById(UUID id) {
        return hitoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hito no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hito> findByEtapaId(Long etapaId) {
        return hitoRepository.findByEtapa_Id(etapaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hito> findByEtapaIdAndEstado(Long etapaId, EstadoHito estado) {
        return hitoRepository.findByEtapa_IdAndEstado(etapaId, estado);
    }

    @Override
    @Transactional
    public Hito update(UUID id, Hito datos) {
        Hito existente = findById(id);
        existente.setTitulo(datos.getTitulo());
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setEstado(datos.getEstado());
        existente.setFechaEstimada(datos.getFechaEstimada());
        return hitoRepository.save(existente);
    }

    @Override
    @Transactional
    public Hito cambiarEstado(UUID id, EstadoHito nuevoEstado) {
        Hito hito = findById(id);
        hito.setEstado(nuevoEstado);
        return hitoRepository.save(hito);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!hitoRepository.existsById(id)) {
            throw new EntityNotFoundException("Hito no encontrado: " + id);
        }
        hitoRepository.deleteById(id);
    }
}
