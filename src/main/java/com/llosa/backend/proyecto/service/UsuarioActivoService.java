package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import java.util.List;

public interface UsuarioActivoService {
    UsuarioActivo findById(Long id);
    List<UsuarioActivo> findByUsuario(Integer usuarioId);
    UsuarioActivo updateCustomerJourney(Long id, String faseComercial, String estadoTramiteLegal);
    UsuarioActivo save(UsuarioActivo usuarioActivo);
    void asignarActivo(AsignarActivoDTO dto);
    List<UsuarioActivo> findByUsuarioEmail(String email);
}
