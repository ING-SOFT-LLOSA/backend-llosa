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
import com.llosa.backend.exception.BusinessException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final FuncionRepository funcionRepository;
    @org.springframework.beans.factory.annotation.Value("${app.dominio-corporativo}")
    private String dominioCorporativo;

    /**
     * NOTA (issue 0000315): Firebase NO participa de la transacción de Spring/JPA.
     * Por eso el orden es importante: primero se intenta todo lo que es
     * transaccional y reversible (validaciones + guardar en Postgres), y
     * RECIÉN AL FINAL se crea el usuario en Firebase, que es la operación
     * externa no transaccional.
     *
     * Si Firebase falla, se hace un rollback manual del registro de Postgres
     * (que ya se había guardado) para no dejar un usuario "fantasma" en BD
     * sin su identidad de autenticación.
     *
     * Si Postgres fallara, nunca se llega a crear nada en Firebase, así que
     * no hay inconsistencia posible en ese sentido.
     */
    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioRequest request) {

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailDuplicadoException(request.getEmail());
        }

        // Validación de dominio corporativo para empleados (issue 0000116)
        if ("EMPLEADO".equals(request.getTipoUsuario())) {
            String email = request.getEmail().toLowerCase().trim();
            String dominioEsperado = "@" + dominioCorporativo.toLowerCase().trim();
            if (!email.endsWith(dominioEsperado)) {
                throw new BusinessException(
                        "El correo de un empleado debe pertenecer al dominio corporativo: " + dominioCorporativo);
            }
        }

        // 1. Armar el perfil (todavía sin firebaseUuid, se completa después)
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setEmail(request.getEmail());
        usuario.setTelefono(request.getTelefono());
        usuario.setDocumentoIdentidad(request.getDocumentoIdentidad());
        usuario.setTipoUsuario(request.getTipoUsuario());
        usuario.setActivo(true);

        if ("CLIENTE".equals(request.getTipoUsuario())) {
            Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                    .orElseThrow(() -> new RecursoNoEncontradoException("Rol CLIENTE no encontrado en la base de datos."));
            usuario.setRol(rolCliente);
        } else if (request.getIdRol() != null) {
            Rol rol = rolRepository.findById(request.getIdRol())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con ID: " + request.getIdRol()));
            usuario.setRol(rol);
        }

        // 2. Crear identidad en Firebase PRIMERO se movió al final (ver abajo).
        //    Antes de eso, intentamos guardar en BD. Si esto falla, Spring
        //    revierte la transacción automáticamente y nunca llegamos a
        //    tocar Firebase: no hay inconsistencia posible en este sentido.
        //
        //    NOTA: firebaseUuid es la clave única en la tabla usuario, así que
        //    no se puede guardar el registro completo sin ella. Para resolver
        //    esto sin reordenar el modelo de datos, usamos un placeholder
        //    temporal único y lo actualizamos inmediatamente después de crear
        //    el usuario en Firebase, dentro de la misma transacción.
        String placeholderUuid = "PENDING_" + java.util.UUID.randomUUID();
        usuario.setFirebaseUuid(placeholderUuid);

        usuario = usuarioRepository.save(usuario);

        // 3. Crear identidad en Firebase (operación externa, no transaccional)
        UserRecord.CreateRequest firebaseRequest = new UserRecord.CreateRequest()
                .setEmail(request.getEmail())
                .setEmailVerified(false)
                .setDisplayName(request.getNombre() + " " + request.getApellidos());

        UserRecord userRecord;
        try {
            userRecord = FirebaseAuth.getInstance().createUser(firebaseRequest);
        } catch (FirebaseAuthException e) {
            // Rollback manual: Firebase falló, así que eliminamos el registro
            // que ya habíamos guardado en Postgres para no dejar un usuario
            // fantasma sin identidad de autenticación.
            usuarioRepository.delete(usuario);
            throw new RuntimeException("Error al crear usuario en Firebase: " + e.getMessage());
        }

        // 4. Reemplazar el placeholder con el UID real de Firebase
        usuario.setFirebaseUuid(userRecord.getUid());
        usuario = usuarioRepository.save(usuario);

        // --- Email de bienvenida / reseteo de contraseña ---
        try {
            sendPasswordResetEmail(request.getEmail());
        } catch (Exception e) {
            log.error("Usuario creado, pero falló el envío del email: {}", e.getMessage());
            // No se revierte la creación del usuario solo porque el email falló.
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

        try {
            FirebaseAuth.getInstance().revokeRefreshTokens(usuario.getFirebaseUuid());
        } catch (FirebaseAuthException e) {
            log.error("No se pudo revocar el refresh token tras cambio de rol para usuario {}: {}",
                    usuarioId, e.getMessage());
        }

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
        try (java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient()) {

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

        } catch (java.io.IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error de comunicación con el servidor de correo: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void eliminarCompletamente(Integer usuarioId) throws Exception {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        FirebaseAuth.getInstance().deleteUser(usuario.getFirebaseUuid());
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
        usuarioRepository.save(usuario);

        return toResponse(usuario);
    }
}