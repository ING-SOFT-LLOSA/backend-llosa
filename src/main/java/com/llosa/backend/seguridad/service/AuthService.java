package com.llosa.backend.seguridad.service;

import com.llosa.backend.seguridad.dto.PerfilConPermisosResponse;
import com.llosa.backend.seguridad.entity.Funcion;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
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
        /* Para testing se invalida esto
        if ("EMPLEADO".equals(usuario.getTipoUsuario())) {
            if (email == null || !email.endsWith("@" + dominioCorporativo)) {
                throw new AccesoDenegadoException("Acceso denegado: dominio no autorizado.");
            }
        }
        */
        List<String> funciones = usuario.getRol() != null
                ? usuario.getRol().getFunciones().stream()
                .map(Funcion::getNombreCodigo)
                .toList()
                : List.of();

        PerfilConPermisosResponse response = new PerfilConPermisosResponse();
        response.setId(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setApellidos(usuario.getApellidos());
        response.setEmail(usuario.getEmail());
        response.setTelefono(usuario.getTelefono());
        response.setTipoUsuario(usuario.getTipoUsuario());
        response.setRol(usuario.getRol() != null ? usuario.getRol().getNombre() : null);
        response.setActivo(usuario.getActivo());
        response.setFunciones(funciones);

        return response;
    }
}