package com.llosa.backend.seguridad.service;

import com.llosa.backend.seguridad.dto.PerfilConPermisosResponse;
import com.llosa.backend.seguridad.entity.Funcion;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    /**
     * NOTA (issue 0000115): la validación de usuario.activo se eliminó de aquí.
     * Ahora se valida de forma centralizada en FirebaseTokenFilter, que se
     * ejecuta para TODOS los endpoints protegidos, no solo /api/auth/me.
     * Si este método se ejecuta, el filtro ya garantizó que el usuario está activo.
     *
     * NOTA (issue 0000116): la validación de dominio corporativo para empleados
     * que estaba aquí (comentada) se removió. Esa validación ahora vive en
     * UsuarioService.crearUsuario(), que es el punto correcto: se valida UNA
     * vez al crear la cuenta, no en cada login.
     */
    public PerfilConPermisosResponse verificarYCargarPerfil(String firebaseUid, String email) {

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no registrado en el sistema"));

        List<String> funciones = usuario.getRol() != null
                ? usuario.getRol().getFunciones().stream()
                .map(Funcion::getNombreCodigo)
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