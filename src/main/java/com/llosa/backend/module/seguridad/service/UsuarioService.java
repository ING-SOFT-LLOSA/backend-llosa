package com.llosa.backend.module.seguridad.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.module.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.module.seguridad.dto.UsuarioResponse;
import com.llosa.backend.module.seguridad.entity.Rol;
import com.llosa.backend.module.seguridad.entity.Usuario;
import com.llosa.backend.module.seguridad.entity.UsuarioResponseFunciones;
import com.llosa.backend.module.seguridad.repository.RolRepository;
import com.llosa.backend.module.seguridad.repository.UsuarioRepository;
import com.llosa.backend.exception.EmailDuplicadoException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioRequest request) throws Exception {

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailDuplicadoException(request.getEmail());
        }

        // 1. Crear identidad en Firebase
        UserRecord.CreateRequest firebaseRequest = new UserRecord.CreateRequest()
                .setEmail(request.getEmail())
                .setEmailVerified(false)
                .setDisplayName(request.getNombre() + " " + request.getApellidos());

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(firebaseRequest);

        // 2. Crear perfil en PostgreSQL
        Usuario usuario = new Usuario();
        usuario.setFirebaseUuid(userRecord.getUid());
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setEmail(request.getEmail());
        usuario.setTelefono(request.getTelefono());
        usuario.setDocumentoIdentidad(request.getDocumentoIdentidad());
        usuario.setTipoUsuario(request.getTipoUsuario());
        usuario.setActivo(true);

        if (request.getIdRol() != null) {
            Rol rol = rolRepository.findById(request.getIdRol())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado"));
            usuario.setRol(rol);
        }

        usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    @Transactional
    public void cambiarEstado(Integer usuarioId, Boolean activo) throws Exception {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        FirebaseAuth.getInstance().revokeRefreshTokens(usuario.getFirebaseUuid());
        FirebaseAuth.getInstance().updateUser(
                new UserRecord.UpdateRequest(usuario.getFirebaseUuid())
                        .setDisabled(!activo));

        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioResponse asignarRol(Integer usuarioId, Integer idRol) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado"));

        usuario.setRol(rol);
        usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponse).toList();
    }

    private UsuarioResponse toResponse(Usuario u) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(u.getId());
        r.setNombre(u.getNombre());
        r.setApellidos(u.getApellidos());
        r.setEmail(u.getEmail());
        r.setTipoUsuario(u.getTipoUsuario());
        r.setRol(u.getRol() != null ? u.getRol().getNombre() : null);
        r.setActivo(u.getActivo());
        r.setCreatedAt(u.getCreatedAt());
        r.setFunciones(u.getRol() != null
                ? u.getRol().getFunciones().stream()
                .map(f -> f.getNombreCodigo()).toList()
                : List.of());
        return r;
    }
}