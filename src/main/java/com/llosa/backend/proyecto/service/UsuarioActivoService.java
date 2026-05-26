package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.UsuarioActivo;
import java.util.List;
import java.util.UUID;

public interface UsuarioActivoService {
    UsuarioActivo findById(Long id);
    List<UsuarioActivo> findByUsuario(Integer usuarioId);
    UsuarioActivo updateCustomerJourney(Long id, String faseComercial, String estadoTramiteLegal);
    UsuarioActivo save(UsuarioActivo usuarioActivo);
}
