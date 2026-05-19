package com.llosa.backend.module.seguridad.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.module.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.module.seguridad.dto.UsuarioResponse;
import com.llosa.backend.module.seguridad.entity.Rol;
import com.llosa.backend.module.seguridad.entity.Usuario;
import com.llosa.backend.module.seguridad.repository.RolRepository;
import com.llosa.backend.module.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
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
            throw new RuntimeException("El correo ya está registrado en el sistema.");
        }

        // 1. Crear identidad en Firebase sin contraseña definida
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
        // Si es CLIENTE, asignar automáticamente el rol CLIENTE
        if ("CLIENTE".equals(request.getTipoUsuario())) {
            Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                    .orElseThrow(() -> new RuntimeException("Rol CLIENTE no encontrado"));
            usuario.setRol(rolCliente);
        } else if (request.getIdRol() != null) {
            Rol rol = rolRepository.findById(request.getIdRol())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
            usuario.setRol(rol);
        }


        usuarioRepository.save(usuario);

        // 3. Firebase envía email automático para que el usuario establezca su contraseña
//        FirebaseAuth.getInstance().generatePasswordResetLink(request.getEmail());
        sendPasswordResetEmail(request.getEmail());
        return toResponse(usuario);
    }

    @Transactional
    public void cambiarEstado(Integer usuarioId, Boolean activo) throws Exception {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

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
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Eliminar de Firebase
        FirebaseAuth.getInstance().deleteUser(usuario.getFirebaseUuid());

        // Eliminar de PostgreSQL
        usuarioRepository.delete(usuario);
    }
}