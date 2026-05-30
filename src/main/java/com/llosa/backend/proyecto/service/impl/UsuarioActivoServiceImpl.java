package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.module.seguridad.entity.Usuario;
import com.llosa.backend.module.seguridad.service.UsuarioService;
import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.proyecto.service.ActivoService;
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
    private final UsuarioService usuarioService;
    private final ActivoService activoService;

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

    @Override
    @Transactional
    public List<UsuarioActivo> findByUsuarioEmail(String email){
        return usuarioActivoRepository.findByUsuarioEmail(email);
    }

    @Override
    @Transactional
    public void asignarActivo(AsignarActivoDTO dto) {
        Usuario usuario = usuarioService.findById(dto.idUsuario());
        Activo activo = activoService.findById(dto.idActivo());
        UsuarioActivo usuarioActivo = UsuarioActivo.builder()
                .usuario(usuario)
                .activo(activo)
                .tipoFinanciamiento(dto.tipoFinanciamiento())
                .faseComercial(dto.faseComercial())
                .estadoTramiteLegal(dto.estadoTramiteLegal())
                .fechaAdquisicion(dto.fechaAdquisicion())
                .build();
        usuarioActivoRepository.save(usuarioActivo);
    }
}
