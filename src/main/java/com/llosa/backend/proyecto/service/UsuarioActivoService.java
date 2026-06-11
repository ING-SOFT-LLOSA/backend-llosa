package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.dto.request.CrearContratoDTO;
import com.llosa.backend.proyecto.dto.response.UsuarioActivoResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.UsuarioActivo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioActivoService {
    UsuarioActivo findById(UUID id);

    /** Retorna todos los procesos comerciales en los que participa el usuario como copropietario. */
    List<UsuarioActivo> findByUsuario(Integer usuarioId);
    List<Activo> findByUsuarioId(Integer usuarioId);
    Optional<UsuarioActivo> findByActivo(UUID activoId);
    UsuarioActivo save(UsuarioActivo usuarioActivo);
    UsuarioActivo crearContratoBase(CrearContratoDTO usuarioActivo);
    UsuarioActivoResponseDTO asignarActivo(AsignarActivoDTO dto);
    void deleteById(UUID id);
    void eliminarContrato(UUID usuarioActivoId);
}
