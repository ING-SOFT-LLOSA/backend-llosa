package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.entity.UsuarioActivo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioActivoService {
    UsuarioActivo findById(UUID id);

    /** Retorna todos los procesos comerciales en los que participa el usuario como copropietario. */
    List<UsuarioActivo> findByUsuario(Integer usuarioId);

    Optional<UsuarioActivo> findByActivo(UUID activoId);
    UsuarioActivo updateCustomerJourney(UUID id, String faseComercial, String estadoTramiteLegal);
    UsuarioActivo save(UsuarioActivo usuarioActivo);
    void asignarActivo(AsignarActivoDTO dto);
    void deleteById(UUID id);
}
