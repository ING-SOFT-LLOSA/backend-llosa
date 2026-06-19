package com.llosa.backend.seguridad.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CP07 - User Creation and Permission Integration Tests
 *
 * Verifica el flujo completo de creación de usuario y asignación de permisos:
 * - Crear usuario con Firebase
 * - Vincular usuario con BD local
 * - Asignar rol y funciones
 * - Refresco de token
 * - Acceso autorizado a endpoints
 */
@Disabled
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class, SecurityTestConfiguration.class, })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
class CP07IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    /**
     * CP07.1: Crear usuario y verificar sincronización Firebase ↔ BD
     *
     * Verifica que:
     * - Firebase crea usuario con UID único
     * - Usuario se guarda en BD con firebaseUuid correcto
     * - Estado inicial es activo
     */
    @Test
    void cp07_crearUsuario_sincronizaFirebaseConBD() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        String firebaseUidGenerado = "cp07-uid-001";

        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn(firebaseUidGenerado);
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(mockRecord);

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            // 1. POST /api/users/register
            mockMvc.perform(post("/api/users/register")
                    .with(securityContext(contextWithAuth(adminAuth())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(req.getEmail()));
        }

        // 2. Verificar en BD
        Usuario usuarioBD = usuarioRepository.findByEmail(req.getEmail()).orElseThrow();
        assertThat(usuarioBD.getFirebaseUuid()).isEqualTo(firebaseUidGenerado);
        assertThat(usuarioBD.getActivo()).isTrue();
        assertThat(usuarioBD.getNombre()).isEqualTo(req.getNombre());
        assertThat(usuarioBD.getApellidos()).isEqualTo(req.getApellidos());
    }

    /**
     * CP07.2: Asignar rol especial y verificar funciones en /me
     *
     * Verifica que:
     * - PUT /api/users/{id}/role asigna rol correctamente
     * - /me devuelve lista de funciones del rol asignado
     */
    @Test
    void cp07_asignarRol_devuelveFuncionesAsociadas() throws Exception {
        // 1. Crear usuario
        Usuario usuario = TestData.usuarioConEmail("cp07-funciones@test.com");
        usuario.setFirebaseUuid("cp07-uid-002");
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // 2. Asignar rol ASESOR (tiene 7 funciones)
        Rol rolAsesor = rolRepository.findByNombre("ASESOR").orElseThrow();
        AsignarRolRequest asignarRolReq = new AsignarRolRequest();
        asignarRolReq.setIdRol(rolAsesor.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", usuarioGuardado.getId())
                .with(securityContext(contextWithAuth(adminAuth())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(asignarRolReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ASESOR"))
                .andExpect(jsonPath("$.funciones.length()").value(7));

        // 3. Consultar /me y verificar funciones
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "cp07-uid-002", "cp07-funciones@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ASESOR"))
                .andExpect(jsonPath("$.funciones.length()").value(7))
                .andExpect(jsonPath("$.funciones[0]").exists());
    }

    /**
     * CP07.3: Refresco de token mantiene permisos
     *
     * Verifica que:
     * - Token actualizado sigue siendo válido
     * - /me devuelve información actualizada del usuario
     */
    @Test
    void cp07_refreshToken_mantienePermisosYPerfil() throws Exception {
        // 1. Crear usuario y asignar rol
        Usuario usuario = TestData.usuarioConEmail("cp07-refresh@test.com");
        usuario.setFirebaseUuid("cp07-uid-003");
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
        AsignarRolRequest asignarRolReq = new AsignarRolRequest();
        asignarRolReq.setIdRol(rolCliente.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", usuarioGuardado.getId())
                .with(securityContext(contextWithAuth(adminAuth())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(asignarRolReq)))
                .andExpect(status().isOk());

        // 2. Usar token original
        FirebaseAuthenticationToken tokenOriginal = new FirebaseAuthenticationToken(
                "cp07-uid-003", "cp07-refresh@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(tokenOriginal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        // 3. Usar token refrescado (simula nuevo JWT con mismo UID)
        FirebaseAuthenticationToken tokenRefrescado = new FirebaseAuthenticationToken(
                "cp07-uid-003", "cp07-refresh@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(tokenRefrescado))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.email").value("cp07-refresh@test.com"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    /**
     * CP07.4: Acceso a endpoints protegidos requiere autenticación
     *
     * Verifica que:
     * - Sin token retorna 403
     * - Con token válido retorna 200
     */
    @Test
    void cp07_endpointsProtegidos_requierenAutenticacion() throws Exception {
        // 1. Sin token → 403
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isForbidden());

        // 2. Con token válido → 200
        Usuario usuario = TestData.usuarioConEmail("cp07-auth@test.com");
        usuario.setFirebaseUuid("cp07-uid-004");
        usuarioRepository.save(usuario);

        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "cp07-uid-004", "cp07-auth@test.com",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("USER_VER")
                ));

        mockMvc.perform(get("/api/users")
                .with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk());
    }

    /**
     * CP07.5: Crear usuario sin rol inicial
     *
     * Verifica que:
     * - Usuario se crea sin rol asignado
     * - /me retorna usuario sin funciones
     * - Rol puede asignarse después
     */
    @Test
    void cp07_crearUsuarioSinRol_puedeAsignarsePosterior() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setEmail("cp07-sinrol@test.com");
        // EMPLEADO sin idRol => queda sin rol asignado. (Un CLIENTE recibiria el
        // rol CLIENTE automaticamente, ver UsuarioService.crearUsuario.)
        req.setTipoUsuario("EMPLEADO");
        req.setIdRol(null);
        String firebaseUid = "cp07-uid-005";

        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn(firebaseUid);
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(mockRecord);

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            mockMvc.perform(post("/api/users/register")
                    .with(securityContext(contextWithAuth(adminAuth())))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        Usuario usuarioBD = usuarioRepository.findByEmail(req.getEmail()).orElseThrow();

        // 1. /me sin rol → funciones vacías
        FirebaseAuthenticationToken tokenSinRol = new FirebaseAuthenticationToken(
                firebaseUid, req.getEmail(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(tokenSinRol))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").doesNotExist())
                .andExpect(jsonPath("$.funciones.length()").value(0));

        // 2. Asignar rol
        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
        AsignarRolRequest asignarRolReq = new AsignarRolRequest();
        asignarRolReq.setIdRol(rolCliente.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", usuarioBD.getId())
                .with(securityContext(contextWithAuth(adminAuth())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(asignarRolReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        // 3. /me con rol → funciones disponibles
        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(tokenSinRol))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.funciones.length()").isNumber());
    }

    /**
     * CP07.6: Cambiar rol de usuario actualiza funciones
     *
     * Verifica que:
     * - Al cambiar rol, las funciones se actualizan automáticamente
     * - /me refleja los cambios inmediatamente
     */
    @Test
    void cp07_cambiarRol_actualizaFunciones() throws Exception {
        // 1. Crear usuario con rol CLIENTE
        Usuario usuario = TestData.usuarioConEmail("cp07-cambiorrol@test.com");
        usuario.setFirebaseUuid("cp07-uid-006");
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
        AsignarRolRequest asignarRolReq = new AsignarRolRequest();
        asignarRolReq.setIdRol(rolCliente.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", usuarioGuardado.getId())
                .with(securityContext(contextWithAuth(adminAuth())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(asignarRolReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "cp07-uid-006", "cp07-cambiorrol@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // 2. Verificar funciones de CLIENTE
        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.funciones").isArray())
                .andExpect(jsonPath("$.funciones[0]").exists()); // CLIENTE tiene funciones

        // 3. Cambiar a rol ASESOR (tiene 7 funciones)
        Rol rolAsesor = rolRepository.findByNombre("ASESOR").orElseThrow();
        asignarRolReq.setIdRol(rolAsesor.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", usuarioGuardado.getId())
                .with(securityContext(contextWithAuth(adminAuth())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(asignarRolReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ASESOR"))
                .andExpect(jsonPath("$.funciones.length()").value(7));

        // 4. Verificar que /me refleja nuevo rol y funciones
        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ASESOR"))
                .andExpect(jsonPath("$.funciones.length()").value(7));
    }

    // ─────────────────────────────────────────────────────────────────────────

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
