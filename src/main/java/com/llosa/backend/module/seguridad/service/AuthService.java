package com.llosa.backend.module.seguridad.service;

import com.llosa.backend.module.seguridad.dto.PerfilConPermisosResponse;
import com.llosa.backend.module.seguridad.entity.Usuario;
import com.llosa.backend.module.seguridad.repository.UsuarioRepository;
import com.llosa.backend.exception.AccesoDenegadoException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    @Value("${app.dominio-corporativo}")
    private String dominioCorporativo;

    public PerfilConPermisosResponse verificarYCargarPerfil(String firebaseUid, String email) {

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no registrado en el sistema"));

        if (!usuario.getActivo()) {
            throw new AccesoDenegadoException("Cuenta suspendida. Contacte a la inmobiliaria.");
        }

        if ("EMPLEADO".equals(usuario.getTipoUsuario())) {
            if (email == null || !email.endsWith("@" + dominioCorporativo)) {
                throw new AccesoDenegadoException("Acceso denegado: dominio no autorizado.");
            }
        }

        List<String> funciones = usuario.getRol() != null
                ? usuario.getRol().getFunciones().stream()
                .map(f -> f.getNombreCodigo())
                .toList()
                : List.of();

        PerfilConPermisosResponse response = new PerfilConPermisosResponse();
        response.setId(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setEmail(usuario.getEmail());
        response.setTipoUsuario(usuario.getTipoUsuario());
        response.setRol(usuario.getRol() != null ? usuario.getRol().getNombre() : null);
        response.setActivo(usuario.getActivo());
        response.setFunciones(funciones);

        return response;
    }
}