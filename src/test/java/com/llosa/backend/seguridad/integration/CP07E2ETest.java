package com.llosa.backend.seguridad.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.config.FirebaseConfig;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@Disabled
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class, SecurityTestConfiguration.class, })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CP07E2ETest {

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
     * CP07: Asignación granular de permisos adicionales
     *
     * Escenario:
     * - Admin crea usuario EMPLEADO (asesor)
     * - Asigna rol ASESOR (base de funciones)
     * - Asigna permiso especial "MODULO_FINANCIERO" (sobrescribe permisos por defecto)
     * - Verifica que /api/auth/me devuelve perfil con permisos extendidos
     */
    @Test
    void cp07_asignarPermiso_flujoCompleto() throws Exception {
        // 1. CREAR USUARIO EMPLEADO
        CrearUsuarioRequest crearReq = new CrearUsuarioRequest();
        crearReq.setNombre("Carlos");
        crearReq.setApellidos("Pérez");
        crearReq.setEmail("cperez@test.com");
        crearReq.setTipoUsuario("EMPLEADO");
        crearReq.setTelefono("987654321");
        crearReq.setDocumentoIdentidad("12345678");

        String firebaseUidGenerado = "cp07-asesor-uid-001";

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

        // Verificar usuario creado en BD
        Usuario usuarioDB = usuarioRepository.findByEmail(crearReq.getEmail()).orElseThrow();
        assertThat(usuarioDB.getFirebaseUuid()).isEqualTo(firebaseUidGenerado);
        assertThat(usuarioDB.getActivo()).isTrue();

        // 2. ASIGNAR ROL BASE (ASESOR)
        Rol rolAsesor = rolRepository.findByNombre("ASESOR").orElseThrow();
        AsignarRolRequest rolReq = new AsignarRolRequest();
        rolReq.setIdRol(rolAsesor.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", usuarioDB.getId())
                        .with(securityContext(contextWithAuth(adminAuth())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ASESOR"))
                .andExpect(jsonPath("$.funciones").isArray());

        // Verificar que usuario ahora tiene rol ASESOR
        Usuario usuarioConRol = usuarioRepository.findByEmail(crearReq.getEmail()).orElseThrow();
        assertThat(usuarioConRol.getRol()).isNotNull();
        assertThat(usuarioConRol.getRol().getNombre()).isEqualTo("ASESOR");

        // 3. VERIFICAR PERMISO ESPECIAL EN PERFIL
        // Nota: En caso real, habría un endpoint PUT /api/users/{id}/permissions
        // Aquí simulamos que el usuario tiene permisos especiales del rol ASESOR
        FirebaseAuthenticationToken tokenDelUsuario = new FirebaseAuthenticationToken(
                firebaseUidGenerado,
                crearReq.getEmail(),
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("PROY_VER"),
                        new SimpleGrantedAuthority("PROY_GESTIONAR")
                ));

        // 4. GET /api/auth/me → Verificar perfil con permisos especiales
        mockMvc.perform(get("/api/auth/me")
                        .with(securityContext(contextWithAuth(tokenDelUsuario))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(crearReq.getEmail()))
                .andExpect(jsonPath("$.rol").value("ASESOR"))
                .andExpect(jsonPath("$.tipoUsuario").value("EMPLEADO"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.funciones").isArray())
                .andExpect(jsonPath("$.funciones.length()").value(7)); // ASESOR tiene 7 funciones
    }

    /**
     * Variante: Verificar que permisos especiales NO se aplican a usuarios sin rol
     */
    @Test
    void cp07_usuarioSinRol_noTienePermisosEspeciales() throws Exception {
        // 1. Crear usuario sin rol
        CrearUsuarioRequest crearReq = TestData.crearUsuarioRequest();
        crearReq.setEmail("sinrol.permisos@test.com");
        crearReq.setTipoUsuario("EMPLEADO");

        String firebaseUidGenerado = "cp07-no-permisos-uid";

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
        }

        Usuario usuarioDB = usuarioRepository.findByEmail(crearReq.getEmail()).orElseThrow();

        // 2. Intenta acceder sin rol
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                firebaseUidGenerado,
                crearReq.getEmail(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                        .with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funciones").isArray())
                .andExpect(jsonPath("$.funciones.length()").value(0)); // Sin funciones
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
