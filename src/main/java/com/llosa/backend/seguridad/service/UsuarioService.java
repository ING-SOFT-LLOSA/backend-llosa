package com.llosa.backend.seguridad.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.seguridad.dto.UpdateUsuarioDTO;
import com.llosa.backend.seguridad.dto.UsuarioResponse;
import com.llosa.backend.seguridad.entity.Funcion;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.RolRepository;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import com.llosa.backend.exception.EmailDuplicadoException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.dto.UsuarioResponseFunciones;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.llosa.backend.seguridad.repository.FuncionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final FuncionRepository funcionRepository;

    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioRequest request) {

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailDuplicadoException(request.getEmail());
        }

        // 1. Crear identidad en Firebase
        UserRecord.CreateRequest firebaseRequest = new UserRecord.CreateRequest()
                .setEmail(request.getEmail())
                .setEmailVerified(false)
                .setDisplayName(request.getNombre() + " " + request.getApellidos());

        UserRecord userRecord;
        try {
            userRecord = FirebaseAuth.getInstance().createUser(firebaseRequest);
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Error al crear usuario en Firebase: " + e.getMessage());
        }

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

        // --- THE RESTORED FLAWLESS ROLE ASSIGNMENT LOGIC ---
        if ("CLIENTE".equals(request.getTipoUsuario())) {
            // Asigna automáticamente el rol CLIENTE si el tipo de usuario es cliente
            Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                    .orElseThrow(() -> new RecursoNoEncontradoException("Rol CLIENTE no encontrado en la base de datos."));
            usuario.setRol(rolCliente);
        } else if (request.getIdRol() != null) {
            // Asigna el rol enviado en el body (para admins, asesores, etc.)
            Rol rol = rolRepository.findById(request.getIdRol())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con ID: " + request.getIdRol()));
            usuario.setRol(rol);
        }

        usuarioRepository.save(usuario);

        // --- THE RESTORED EMAIL TRIGGER ---
        try {
            sendPasswordResetEmail(request.getEmail());
        } catch (Exception e) {
            log.error("Usuario creado, pero falló el envío del email: {}", e.getMessage());
            // Optional: You can choose to throw an exception here, but usually,
            // you don't want to rollback the user creation just because the email failed.
        }

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

    public UsuarioResponse toResponse(Usuario u) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(u.getId());
        r.setNombre(u.getNombre());
        r.setApellidos(u.getApellidos());
        r.setEmail(u.getEmail());
        r.setDocumentoIdentidad(u.getDocumentoIdentidad());
        r.setTelefono(u.getTelefono());
        r.setTipoUsuario(u.getTipoUsuario());
        r.setRol(u.getRol() != null ? u.getRol().getNombre() : null);
        r.setActivo(u.getActivo());
        r.setCreatedAt(u.getCreatedAt());
        r.setFunciones(u.getRol() != null
                ? u.getRol().getFunciones().stream()
                .map(Funcion::getNombreCodigo).toList()
                : List.of());
        return r;
    }

    private void sendPasswordResetEmail(String email) throws Exception {
        String apiKey = "AIzaSyDo_yQ7_tJ3kulCZXaqOcPXAzywtF4pAj0";
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + apiKey;

        String body = "{\"requestType\":\"PASSWORD_RESET\",\"email\":\"" + email + "\"}";

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();

        java.net.http.HttpResponse<String> response = client.send(httpRequest,
                java.net.http.HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error al enviar email de bienvenida: " + response.body());
        }
    }

    @Transactional
    public void eliminarCompletamente(Integer usuarioId) throws Exception {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        // Eliminar de Firebase
        FirebaseAuth.getInstance().deleteUser(usuario.getFirebaseUuid());

        // Eliminar de PostgreSQL
        usuarioRepository.delete(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario findById(Integer id){
        return usuarioRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Usuario no encontrado")
        );
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponseFunciones> listarPaginadoYFiltrado(String search, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        Page<Usuario> usuariosPage = usuarioRepository.buscarUsuariosPaginados(search, pageable);
        return usuariosPage.map(UsuarioResponseFunciones::fromEntity);
    }

    @Transactional(readOnly = true)
    public Usuario findByFirebaseUuid(String firebaseUuid) {
        return usuarioRepository.findByFirebaseUuid(firebaseUuid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    @Transactional
    public UsuarioResponse actualizarUsuario(
            Integer id,
            UpdateUsuarioDTO request) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (request.getNombre() != null) {
            usuario.setNombre(request.getNombre());
        }

        if (request.getApellidos() != null) {
            usuario.setApellidos(request.getApellidos());
        }

        if (request.getTelefono() != null) {
            usuario.setTelefono(request.getTelefono());
        }

        if (request.getEmail() != null) {
            usuario.setEmail(request.getEmail());
        }
        if (request.getDocumentoIdentidad() != null) {
            usuario.setDocumentoIdentidad(request.getDocumentoIdentidad());
        }
        if (request.getTipoUsuario() != null) {
            usuario.setTipoUsuario(request.getTipoUsuario());
        }

        usuarioRepository.save(usuario);

        return toResponse(usuario);
    }
    @Transactional
    public UsuarioResponse modificarFunciones(Integer usuarioId, List<Integer> idFunciones) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Rol rol = usuario.getRol();
        if (rol == null) {
            throw new RecursoNoEncontradoException("El usuario no tiene un rol asignado");
        }

        List<Funcion> nuevasFunciones = funcionRepository.findAllById(idFunciones);
        if (nuevasFunciones.size() != idFunciones.size()) {
            throw new RecursoNoEncontradoException("Una o más funciones no existen");
        }

        rol.setFunciones(nuevasFunciones);
        // rol ya está managed por JPA, no hace falta llamar save explícito,
        // pero lo llamamos para ser explícitos
        usuarioRepository.save(usuario);

        return toResponse(usuario);
    }
}