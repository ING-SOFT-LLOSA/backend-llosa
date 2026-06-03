package com.llosa.backend.module.seguridad.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.module.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.module.seguridad.dto.UsuarioResponse;
import com.llosa.backend.module.seguridad.entity.Funcion;
import com.llosa.backend.module.seguridad.entity.Rol;
import com.llosa.backend.module.seguridad.entity.Usuario;
import com.llosa.backend.module.seguridad.entity.UsuarioResponseFunciones;
import com.llosa.backend.module.seguridad.repository.RolRepository;
import com.llosa.backend.module.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.firebase.auth.FirebaseAuthException;
import com.llosa.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioRequest request) {

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("El correo ya está registrado en el sistema.", HttpStatus.CONFLICT);
        }

        UserRecord userRecord;
        try {
            UserRecord.CreateRequest firebaseRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getEmail())
                    .setEmailVerified(false)
                    .setDisplayName(request.getNombre() + " " + request.getApellidos());
            userRecord = FirebaseAuth.getInstance().createUser(firebaseRequest);
        } catch (FirebaseAuthException e) {
            throw new ApiException("Error al crear usuario en Firebase: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Usuario usuario = new Usuario();
        usuario.setFirebaseUuid(userRecord.getUid());
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setEmail(request.getEmail());
        usuario.setTelefono(request.getTelefono());
        usuario.setDocumentoIdentidad(request.getDocumentoIdentidad());
        usuario.setTipoUsuario(request.getTipoUsuario());
        usuario.setActivo(true);

        if ("CLIENTE".equals(request.getTipoUsuario())) {
            Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                    .orElseThrow(() -> new ApiException("Rol CLIENTE no encontrado.",
                            HttpStatus.INTERNAL_SERVER_ERROR));
            usuario.setRol(rolCliente);
        } else if (request.getIdRol() != null) {
            Rol rol = rolRepository.findById(request.getIdRol())
                    .orElseThrow(() -> new ApiException("Rol no encontrado.",
                            HttpStatus.NOT_FOUND));
            usuario.setRol(rol);
        }

        usuarioRepository.save(usuario);

        try {
            sendPasswordResetEmail(request.getEmail());
        } catch (Exception e) {
            throw new ApiException("Usuario creado pero falló el envío del email: " + e.getMessage(),
                    HttpStatus.CREATED);
        }

        return toResponse(usuario);
    }

    @Transactional
    public void cambiarEstado(Integer usuarioId, Boolean activo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ApiException("Usuario no encontrado.", HttpStatus.NOT_FOUND));

        try {
            FirebaseAuth.getInstance().revokeRefreshTokens(usuario.getFirebaseUuid());
            FirebaseAuth.getInstance().updateUser(
                    new UserRecord.UpdateRequest(usuario.getFirebaseUuid())
                            .setDisabled(!activo));
        } catch (FirebaseAuthException e) {
            throw new ApiException("Error al actualizar estado en Firebase: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

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
                .map(Funcion::getNombreCodigo).toList()
                : List.of());
        return r;
    }
    private void sendPasswordResetEmail(String email) throws Exception {
        String apiKey = "AIzaSyDo_yQ7_tJ3kulCZXaqOcPXAzywtF4pAj0";
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + apiKey;
        if (!usuarioRepository.existsByEmail(email)){
            throw new RuntimeException("No existe un usuario con ese email ") ;
        }

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

    @Transactional
    public Usuario findById(Integer id){
        return usuarioRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Usuario no encontrado")
        );
    }

    public Page<UsuarioResponseFunciones> listarPaginadoYFiltrado(String search, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        Page<Usuario> usuariosPage = usuarioRepository.buscarUsuariosPaginados(search, pageable);
        return usuariosPage.map(UsuarioResponseFunciones::fromEntity);
    }

}