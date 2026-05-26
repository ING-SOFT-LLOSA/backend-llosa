package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.proyecto.service.UsuarioActivoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioActivoServiceImpl implements UsuarioActivoService {

    private final UsuarioActivoRepository usuarioActivoRepository;

    @Override
    @Transactional(readOnly = true)
    public UsuarioActivo findById(Long id) {
        return usuarioActivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expediente de Usuario-Activo no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioActivo> findByUsuario(Integer usuarioId) {
        return usuarioActivoRepository.findByUsuario_Id(usuarioId);
    }

    @Override
    @Transactional
    public UsuarioActivo updateCustomerJourney(Long id, String faseComercial, String estadoTramiteLegal) {
        UsuarioActivo existente = findById(id);
        if (faseComercial != null) existente.setFaseComercial(faseComercial);
        if (estadoTramiteLegal != null) existente.setEstadoTramiteLegal(estadoTramiteLegal);
        return usuarioActivoRepository.save(existente);
    }

    @Override
    @Transactional
    public UsuarioActivo save(UsuarioActivo usuarioActivo) {
        return usuarioActivoRepository.save(usuarioActivo);
    }
}
