package com.llosa.backend.module.seguridad.entity;

import java.time.LocalDateTime;
import java.util.List;

public record UsuarioResponseFunciones(
        Integer id,
        String nombre,
        String apellidos,
        String email,
        String tipoUsuario,
        String documentoIdentidad,
        String telefono,
        Rol rol,
        Boolean activo,
        LocalDateTime createdAt,
        List<Funcion> funciones
) {
    public static UsuarioResponseFunciones fromEntity(Usuario u) {
        List<Funcion> listaFunciones = (u.getRol() != null)
                ? u.getRol().getFunciones()
                : List.of();
        return new UsuarioResponseFunciones(
                u.getId(),
                u.getNombre(),
                u.getApellidos(),
                u.getEmail(),
                u.getTipoUsuario(),
                u.getDocumentoIdentidad(),
                u.getTelefono(),
                u.getRol(),
                u.getActivo(),
                u.getCreatedAt(),
                listaFunciones
        );
    }
}
