package com.llosa.backend.seguridad.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.GcsTestConfig;
import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.seguridad.dto.AsignarRolRequest;
import com.llosa.backend.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.RolRepository;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import org.springframework.transaction.annotation.Transactional;
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

/**
 * CP08 E2E Test: Verificar que al desactivar un usuario se invaliden sus tokens JWT
 * y se cierren sus sesiones
 *
 * Flujo:
 * 1. POST /api/users/register → Crear usuario EMPLEADO
 * 2. PUT /api/users/{id}/role → Asignar rol
 * 3. GET /api/auth/me → Verificar que acceso funciona (200 OK)
 * 4. DELETE /api/users/{id} → Desactivar usuario
 * 5. GET /api/auth/me → Verificar que acceso es denegado (403 Forbidden)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class, SecurityTestConfiguration.class, GcsTestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
class CP08E2ETest {

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
    void setup() {
        usuarioRepository.deleteAll();
    }

    /**
     * CP08: Desactivación de usuario invalida tokens y cierra sesiones
     *
     * Escenario:
     * - Admin crea usuario EMPLEADO
     * - Asigna rol para que tenga permisos
     * - Usuario puede acceder a /api/auth/me (200 OK)
     * - Admin desactiva al usuario (DELETE /api/users/{id})
     * - Usuario intenta acceder a /api/auth/me → Recibe 403 Forbidden
     * - Mensaje de error: "Cuenta suspendida. Contacte a la inmobiliaria."
     */
    @Test
    void cp08_desactivarUsuario_invalida_tokens_flujoCompleto() throws Exception {
        // 1. CREAR USUARIO EMPLEADO
        CrearUsuarioRequest crearReq = new CrearUsuarioRequest();
        crearReq.setNombre("Ricardo");
        crearReq.setApellidos("López");
        crearReq.setEmail("rlopez@test.com");
        crearReq.setTipoUsuario("EMPLEADO");
        crearReq.setTelefono("999888777");
        crearReq.setDocumentoIdentidad("87654321");

        String firebaseUidGenerado = "cp08-empleado-uid-001";

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
                    .andExpect(status().isOk());

            Usuario usuarioDB = usuarioRepository.findByEmail(crearReq.getEmail()).orElseThrow();
            assertThat(usuarioDB.getActivo()).isTrue();

            // 2. ASIGNAR ROL para que tenga permisos
            Rol rolTecnico = rolRepository.findByNombre("TECNICO").orElseThrow();
            AsignarRolRequest rolReq = new AsignarRolRequest();
            rolReq.setIdRol(rolTecnico.getIdRol());

            mockMvc.perform(put("/api/users/{id}/role", usuarioDB.getId())
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rolReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("TECNICO"));

            // 3. VERIFICAR QUE USUARIO PUEDE ACCEDER (PRE-DESACTIVACIÓN)
            FirebaseAuthenticationToken tokenDelUsuario = new FirebaseAuthenticationToken(
                    firebaseUidGenerado,
                    crearReq.getEmail(),
                    List.of(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("PROY_VER")
                    ));

            mockMvc.perform(get("/api/auth/me")
                            .with(securityContext(contextWithAuth(tokenDelUsuario))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(crearReq.getEmail()))
                    .andExpect(jsonPath("$.activo").value(true));

            // 4. DESACTIVAR USUARIO
            mockMvc.perform(delete("/api/users/{id}", usuarioDB.getId())
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf()))
                    .andExpect(status().isOk());

            // Verificar que usuario está desactivado en BD
            Usuario usuarioDesactivado = usuarioRepository.findById(usuarioDB.getId()).orElseThrow();
            assertThat(usuarioDesactivado.getActivo()).isFalse();

            // 5. INTENTA ACCEDER DESPUÉS DE DESACTIVACIÓN → 403 FORBIDDEN
            mockMvc.perform(get("/api/auth/me")
                            .with(securityContext(contextWithAuth(tokenDelUsuario))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Cuenta suspendida. Contacte a la inmobiliaria."));
        }
    }

    /**
     * Variante: Reactivación de usuario
     * Después de desactivar, se reactiva el usuario y verifica que acceso funciona nuevamente
     */
    @Test
    void cp08_reactivarUsuario_restaura_acceso() throws Exception {
        // 1. Crear usuario
        CrearUsuarioRequest crearReq = TestData.crearUsuarioRequest();
        crearReq.setEmail("reactivar.test@test.com");
        crearReq.setTipoUsuario("EMPLEADO");

        String firebaseUidGenerado = "cp08-reactivar-uid";

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
                    .andExpect(status().isOk());

            Usuario usuarioDB = usuarioRepository.findByEmail(crearReq.getEmail()).orElseThrow();

            // 2. Asignar rol
            Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
            AsignarRolRequest rolReq = new AsignarRolRequest();
            rolReq.setIdRol(rolCliente.getIdRol());

            mockMvc.perform(put("/api/users/{id}/role", usuarioDB.getId())
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rolReq)))
                    .andExpect(status().isOk());

            FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                    firebaseUidGenerado,
                    crearReq.getEmail(),
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            // 3. Verificar acceso (pre-desactivación)
            mockMvc.perform(get("/api/auth/me")
                            .with(securityContext(contextWithAuth(token))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activo").value(true));

            // 4. Desactivar
            mockMvc.perform(delete("/api/users/{id}", usuarioDB.getId())
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf()))
                    .andExpect(status().isOk());

            // 5. Verificar acceso denegado (post-desactivación)
            mockMvc.perform(get("/api/auth/me")
                            .with(securityContext(contextWithAuth(token))))
                    .andExpect(status().isForbidden());

            // 6. REACTIVAR (actualizar BD directamente en test)
            Usuario guardado = usuarioRepository.findById(usuarioDB.getId()).orElseThrow();
            guardado.setActivo(true);
            usuarioRepository.save(guardado);

            // 7. Verificar acceso restaurado
            mockMvc.perform(get("/api/auth/me")
                            .with(securityContext(contextWithAuth(token))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(crearReq.getEmail()))
                    .andExpect(jsonPath("$.activo").value(true));
        }
    }

    // ─────────────────────────────────────────────────────────────────

    private static FirebaseAuthenticationToken adminAuth() {
        return new FirebaseAuthenticationToken(
                "admin-uid", "admin@llosaedificaciones.com",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("USER_GESTIONAR"),
                        new SimpleGrantedAuthority("USER_VER"),
                        new SimpleGrantedAuthority("ROL_GESTIONAR")
                ));
    }

    private static SecurityContext contextWithAuth(FirebaseAuthenticationToken auth) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
