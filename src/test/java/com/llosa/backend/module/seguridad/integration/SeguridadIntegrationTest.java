package com.llosa.backend.module.seguridad.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.module.seguridad.dto.AsignarRolRequest;
import com.llosa.backend.module.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.module.seguridad.entity.Rol;
import com.llosa.backend.module.seguridad.entity.Usuario;
import com.llosa.backend.module.seguridad.repository.RolRepository;
import com.llosa.backend.module.seguridad.repository.UsuarioRepository;
import com.llosa.backend.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class, SecurityTestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SeguridadIntegrationTest {

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    RolRepository rolRepository;

    @BeforeEach
    void limpiarUsuarios() {
        usuarioRepository.deleteAll();
    }

    // ── Flujo completo: crear → asignar rol → consultar /me ──────────────────

    @Test
    void flujoCompleto_crearUsuario_asignarRol_consultarPerfil() throws Exception {
        // 1. Crear usuario vía HTTP (mock Firebase.createUser)
        CrearUsuarioRequest crearReq = TestData.crearUsuarioRequest();
        String firebaseUidGenerado = "integration-uid-001";

        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn(firebaseUidGenerado);
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(mockRecord);

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            mockMvc.perform(post("/api/users/register")
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(crearReq.getEmail()));
        }

        // 2. Verificar que el usuario quedó en BD con el UID de Firebase
        Usuario usuarioDB = usuarioRepository.findByEmail(crearReq.getEmail()).orElseThrow();
        assertThat(usuarioDB.getFirebaseUuid()).isEqualTo(firebaseUidGenerado);

        // 3. Asignar rol CLIENTE al usuario
        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
        AsignarRolRequest rolReq = new AsignarRolRequest();
        rolReq.setIdRol(rolCliente.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", usuarioDB.getId())
                        .with(securityContext(contextWithAuth(adminAuth())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        // 4. Consultar /api/auth/me con el uid de ese usuario
        FirebaseAuthenticationToken tokenDelUsuario = new FirebaseAuthenticationToken(
                firebaseUidGenerado,
                crearReq.getEmail(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                        .with(securityContext(contextWithAuth(tokenDelUsuario))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(crearReq.getEmail()))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.funciones").isArray())
                .andExpect(jsonPath("$.funciones[0]").exists());
    }

    // ── Desactivar usuario → /me rechazado con 403 ───────────────────────────

    @Test
    void usuarioDesactivado_meDevuelve403() throws Exception {
        Usuario u = TestData.usuarioConEmail("suspendido@test.com");
        u.setFirebaseUuid("uid-suspendido");
        usuarioRepository.save(u);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            mockMvc.perform(delete("/api/users/{id}", u.getId())
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        FirebaseAuthenticationToken tokenSuspendido = new FirebaseAuthenticationToken(
                "uid-suspendido", "suspendido@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").with(securityContext(contextWithAuth(tokenSuspendido))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Cuenta suspendida. Contacte a la inmobiliaria."));
    }

    // ── Registro con email duplicado → 409 ───────────────────────────────────

    @Test
    void registrarEmailDuplicado_devuelve409() throws Exception {
        Usuario existente = TestData.usuarioConEmail("duplicado@test.com");
        usuarioRepository.save(existente);

        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setEmail("duplicado@test.com");

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            mockMvc.perform(post("/api/users/register")
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").exists());

            ms.verifyNoInteractions();
        }
    }

    // ── Brecha de seguridad: usuario suspendido accede a otros endpoints ──────

    @Test
    void usuarioSuspendido_puedeAccederEndpointsSinCheckActivo() throws Exception {
        // SECURITY GAP: el FirebaseTokenFilter solo valida el token Firebase,
        // NO verifica el campo activo en BD. La suspensión solo se chequea en
        // AuthService.verificarYCargarPerfil (/api/auth/me).
        // Un usuario suspendido con token válido puede seguir llamando otros endpoints.
        Usuario suspendido = TestData.usuarioConEmail("brecha@test.com");
        suspendido.setFirebaseUuid("uid-brecha");
        suspendido.setActivo(false);
        usuarioRepository.save(suspendido);

        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "uid-brecha", "brecha@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/roles").with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk()); // debería ser 403 — brecha conocida
    }

    // ── Listar usuarios ───────────────────────────────────────────────────────

    @Test
    void listarUsuarios_devuelveTodosLosRegistrados() throws Exception {
        usuarioRepository.save(TestData.usuarioConEmail("a@test.com"));
        usuarioRepository.save(TestData.usuarioConEmail("b@test.com"));

        mockMvc.perform(get("/api/users").with(securityContext(contextWithAuth(adminAuth()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── Listar roles con funciones ────────────────────────────────────────────

    @Test
    void listarRoles_devuelveSeedsConFunciones() throws Exception {
        mockMvc.perform(get("/api/roles").with(securityContext(contextWithAuth(adminAuth()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    // ── Usuario sin rol → /me devuelve perfil con funciones vacías ────────────

    @Test
    void usuarioSinRol_me_devuelvePerfilConFuncionesVacias() throws Exception {
        Usuario u = TestData.usuarioConEmail("sinrol@test.com");
        u.setFirebaseUuid("uid-sinrol");
        usuarioRepository.save(u);

        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "uid-sinrol", "sinrol@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sinrol@test.com"))
                .andExpect(jsonPath("$.funciones").isArray())
                .andExpect(jsonPath("$.funciones.length()").value(0));
    }

    // ── UID de Firebase no registrado en BD → 404 ────────────────────────────

    @Test
    void usuarioNoRegistrado_me_devuelve404() throws Exception {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "uid-fantasma", "fantasma@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").with(securityContext(contextWithAuth(token))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Usuario no registrado en el sistema"));
    }

    // ── DELETE usuario inexistente → 404 ─────────────────────────────────────

    @Test
    void usuarioInexistente_delete_devuelve404() throws Exception {
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            mockMvc.perform(delete("/api/users/{id}", 99999)
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // ── PUT role usuario inexistente → 404 ───────────────────────────────────

    @Test
    void usuarioInexistente_asignarRol_devuelve404() throws Exception {
        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
        AsignarRolRequest req = new AsignarRolRequest();
        req.setIdRol(rolCliente.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", 99999)
                        .with(securityContext(contextWithAuth(adminAuth())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── Reactivar usuario suspendido → /me vuelve a funcionar ────────────────

    @Test
    void usuarioReactivado_meDevuelve200() throws Exception {
        Usuario u = TestData.usuarioConEmail("reactivar@test.com");
        u.setFirebaseUuid("uid-reactivar");
        usuarioRepository.save(u);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            // 1. Desactivar
            mockMvc.perform(delete("/api/users/{id}", u.getId())
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf()))
                    .andExpect(status().isOk());

            // 2. /me rechazado
            FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                    "uid-reactivar", "reactivar@test.com",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            mockMvc.perform(get("/api/auth/me").with(securityContext(contextWithAuth(token))))
                    .andExpect(status().isForbidden());
        }

        // 3. Reactivar via PUT /api/users/{id}/activo — pero el endpoint es DELETE para desactivar.
        //    La reactivación se hace directamente en BD para este test.
        Usuario guardado = usuarioRepository.findById(u.getId()).orElseThrow();
        guardado.setActivo(true);
        usuarioRepository.save(guardado);

        // 4. /me vuelve a funcionar
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "uid-reactivar", "reactivar@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        mockMvc.perform(get("/api/auth/me").with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("reactivar@test.com"));
    }

    // ── ASESOR: asignar rol → /me devuelve exactamente 6 funciones ───────────

    @Test
    void asignarRolAsesor_meDevuelveSeisFunciones() throws Exception {
        Usuario u = TestData.usuarioConEmail("asesor-it@test.com");
        u.setFirebaseUuid("uid-asesor-it");
        usuarioRepository.save(u);

        Rol rolAsesor = rolRepository.findByNombre("ASESOR").orElseThrow();
        AsignarRolRequest req = new AsignarRolRequest();
        req.setIdRol(rolAsesor.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", u.getId())
                        .with(securityContext(contextWithAuth(adminAuth())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ASESOR"))
                .andExpect(jsonPath("$.funciones.length()").value(6));

        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "uid-asesor-it", "asesor-it@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ASESOR"))
                .andExpect(jsonPath("$.funciones.length()").value(6));
    }

    // ── Asignar rol inexistente → 404 ────────────────────────────────────────

    @Test
    void asignarRolInexistente_devuelve404() throws Exception {
        Usuario u = TestData.usuarioConEmail("roltest@test.com");
        usuarioRepository.save(u);

        AsignarRolRequest req = new AsignarRolRequest();
        req.setIdRol(9999);

        mockMvc.perform(put("/api/users/{id}/role", u.getId())
                        .with(securityContext(contextWithAuth(adminAuth())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static FirebaseAuthenticationToken adminAuth() {
        return new FirebaseAuthenticationToken(
                "admin-uid", "admin@llosaedificaciones.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static SecurityContext contextWithAuth(FirebaseAuthenticationToken auth) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
