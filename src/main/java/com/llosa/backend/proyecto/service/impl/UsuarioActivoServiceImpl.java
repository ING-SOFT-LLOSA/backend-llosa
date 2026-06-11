package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.service.UsuarioService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioActivoServiceImpl implements UsuarioActivoService {

    private final UsuarioActivoRepository usuarioActivoRepository;
    private final UsuarioService usuarioService;
    private final ActivoService activoService;

    @Override
    @Transactional(readOnly = true)
    public UsuarioActivo findById(UUID id) {
        return usuarioActivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expediente de Usuario-Activo no encontrado: " + id));
    }

    /**
     * Retorna todos los procesos comerciales donde el usuario participa como copropietario.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioActivo> findByUsuario(Integer usuarioId) {
        return usuarioActivoRepository.findByClienteId(usuarioId);
    }

    @Override
    @Transactional
    public UsuarioActivo updateCustomerJourney(UUID id, String faseComercial, String estadoTramiteLegal) {
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

    /**
     * Crea el proceso comercial activo y vincula la lista de copropietarios recibida en el DTO.
     */
    @Override
    @Transactional
    public void asignarActivo(AsignarActivoDTO dto) {
        // Resolver cada copropietario y validar su existencia
        List<Usuario> clientes = new ArrayList<>();
        for (Integer idUsuario : dto.idsUsuarios()) {
            clientes.add(usuarioService.findById(idUsuario));
        }

        Activo activo = activoService.findById(dto.idActivo());

        UsuarioActivo usuarioActivo = UsuarioActivo.builder()
                .activo(activo)
                .clientes(clientes)
                .tipoFinanciamiento(dto.tipoFinanciamiento())
                .faseComercial(dto.faseComercial())
                .estadoTramiteLegal(dto.estadoTramiteLegal())
                .fechaAdquisicion(dto.fechaAdquisicion())
                .build();

        usuarioActivoRepository.save(usuarioActivo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioActivo> findByActivo(UUID activoId) {
        return usuarioActivoRepository.findByActivo_Id(activoId);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        usuarioActivoRepository.deleteById(id);
    }
}
