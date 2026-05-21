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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import static org.assertj.core.api.Assertions.*;
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

    @MockBean
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

    // ── Desactivar usuario → /me rechazado ───────────────────────────────────

    @Test
    void usuarioDesactivado_meDevuelve500() throws Exception {
        // Crear usuario directamente en DB
        Usuario u = TestData.usuarioConEmail("suspendido@test.com");
        u.setFirebaseUuid("uid-suspendido");
        usuarioRepository.save(u);

        // Desactivar (mock Firebase)
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            mockMvc.perform(delete("/api/users/{id}", u.getId())
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        // Consultar /me → "Cuenta suspendida"
        FirebaseAuthenticationToken tokenSuspendido = new FirebaseAuthenticationToken(
                "uid-suspendido", "suspendido@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThatThrownBy(() ->
                mockMvc.perform(get("/api/auth/me").with(securityContext(contextWithAuth(tokenSuspendido)))))
                .rootCause()
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("suspendida");
    }

    // ── Registro con email duplicado ─────────────────────────────────────────

    @Test
    void registrarEmailDuplicado_lanzaExcepcion() throws Exception {
        Usuario existente = TestData.usuarioConEmail("duplicado@test.com");
        usuarioRepository.save(existente);

        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setEmail("duplicado@test.com");

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            assertThatThrownBy(() ->
                    mockMvc.perform(post("/api/users/register")
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .rootCause()
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ya está registrado");

            ms.verifyNoInteractions();
        }
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
