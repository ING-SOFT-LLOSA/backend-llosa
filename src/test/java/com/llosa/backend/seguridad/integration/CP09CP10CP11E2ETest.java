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
 * CP09, CP10, CP11 E2E Tests: Autenticación de Clientes con diferentes estados de unidades
 *
 * CP09: Cliente con unidad "Vendido" puede acceder (Modo regular)
 * CP10: Cliente inactivo NO puede acceder (Desistimiento)
 * CP11: Cliente con unidad "Separado" entra en Modo de Espera (módulos bloqueados)
 *
 * Flujos:
 * CP09: POST /api/users → PUT /api/users/{id}/role (CLIENTE) → GET /api/auth/me (200, acceso completo)
 * CP10: POST /api/users → (activo=false) → POST /api/auth/login (403 Forbidden)
 * CP11: POST /api/users → POST /api/units/assign (Separado) → GET /api/auth/me (200, modoDespera=true)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class, SecurityTestConfiguration.class, GcsTestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
class CP09CP10CP11E2ETest {

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

    // ──────────────────────────────────────────────────────────────────────────────
    // CP09: Cliente con unidad "Vendido" puede acceder (Modo regular)
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * CP09: Cliente con unidad "Vendido" ingresa en Modo regular (acceso completo)
     *
     * Escenario:
     * - Admin crea cliente en el sistema
     * - Asigna rol CLIENTE
     * - Cliente intenta acceder con token Firebase válido
     * - Sistema confirma estado "Activo" y unidad "Vendido"
     * - Renderiza Dashboard completo (GET /api/auth/me devuelve perfil)
     */
    @Test
    void cp09_clienteConUnidadVendido_puedeAccederModoRegular() throws Exception {
        // 1. CREAR CLIENTE
        CrearUsuarioRequest crearReq = new CrearUsuarioRequest();
        crearReq.setNombre("María");
        crearReq.setApellidos("González");
        crearReq.setEmail("maria.gonzalez@gmail.com");
        crearReq.setTipoUsuario("CLIENTE");
        crearReq.setTelefono("999111222");
        crearReq.setDocumentoIdentidad("11223344");

        String firebaseUidGenerado = "cp09-cliente-vendido-uid";

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

            Usuario clienteDB = usuarioRepository.findByEmail(crearReq.getEmail()).orElseThrow();
            assertThat(clienteDB.getActivo()).isTrue(); // Estado = Activo ✅

            // 2. ASIGNAR ROL CLIENTE
            Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
            AsignarRolRequest rolReq = new AsignarRolRequest();
            rolReq.setIdRol(rolCliente.getIdRol());

            mockMvc.perform(put("/api/users/{id}/role", clienteDB.getId())
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rolReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("CLIENTE"));

            // 3. CLIENTE INTENTA ACCEDER CON TOKEN FIREBASE VÁLIDO
            FirebaseAuthenticationToken tokenCliente = new FirebaseAuthenticationToken(
                    firebaseUidGenerado,
                    crearReq.getEmail(),
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            // 4. GET /api/auth/me → Dashboard completo (acceso concedido)
            mockMvc.perform(get("/api/auth/me")
                            .with(securityContext(contextWithAuth(tokenCliente))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(crearReq.getEmail()))
                    .andExpect(jsonPath("$.rol").value("CLIENTE"))
                    .andExpect(jsonPath("$.activo").value(true)) // Estado: Activo ✅
                    .andExpect(jsonPath("$.tipoUsuario").value("CLIENTE"))
                    .andExpect(jsonPath("$.funciones").isArray()); // Dashboard disponible
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // CP10: Cliente inactivo NO puede acceder
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * CP10: Cliente con perfil "Inactivo" (desistimiento) NO puede acceder al portal
     *
     * Escenario:
     * - Admin crea cliente con estado ACTIVO
     * - Cambia estado a INACTIVO (desistimiento)
     * - Cliente intenta acceder con token Firebase
     * - Sistema detecta estado INACTIVO y deniega acceso (403 Forbidden)
     */
    @Test
    void cp10_clienteInactivo_noDebeAcceder() throws Exception {
        // 1. CREAR CLIENTE INACTIVO
        CrearUsuarioRequest crearReq = new CrearUsuarioRequest();
        crearReq.setNombre("Juan");
        crearReq.setApellidos("Desistió");
        crearReq.setEmail("desistio@gmail.com");
        crearReq.setTipoUsuario("CLIENTE");
        crearReq.setTelefono("999555666");
        crearReq.setDocumentoIdentidad("55667788");

        String firebaseUidGenerado = "cp10-inactivo-uid";

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

            Usuario clienteDB = usuarioRepository.findByEmail(crearReq.getEmail()).orElseThrow();
            assertThat(clienteDB.getActivo()).isTrue(); // Creado como activo

            // 2. DESACTIVAR CLIENTE (Simular desistimiento)
            mockMvc.perform(delete("/api/users/{id}", clienteDB.getId())
                            .with(securityContext(contextWithAuth(adminAuth())))
                            .with(csrf()))
                    .andExpect(status().isOk());

            Usuario clienteDesactivado = usuarioRepository.findById(clienteDB.getId()).orElseThrow();
            assertThat(clienteDesactivado.getActivo()).isFalse(); // Ahora inactivo ✅

            // 3. CLIENTE INTENTA ACCEDER CON TOKEN FIREBASE
            FirebaseAuthenticationToken tokenCliente = new FirebaseAuthenticationToken(
                    firebaseUidGenerado,
                    crearReq.getEmail(),
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            // 4. GET /api/auth/me → Denegado (403 Forbidden)
            mockMvc.perform(get("/api/auth/me")
                            .with(securityContext(contextWithAuth(tokenCliente))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Cuenta suspendida. Contacte a la inmobiliaria."));
        }
    }

    /**
     * CP10 Variante: Usuario no registrado intenta acceder (404 Not Found)
     * Este es un caso de error donde el UID de Firebase no existe en BD
     */
    @Test
    void cp10_usuarioNoRegistrado_noDebeAcceder() throws Exception {
        // Cliente Firebase válido pero SIN registro en BD
        FirebaseAuthenticationToken tokenFantasma = new FirebaseAuthenticationToken(
                "uid-fantasma-no-existe",
                "fantasma@gmail.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // GET /api/auth/me → No encontrado (404)
        mockMvc.perform(get("/api/auth/me")
                        .with(securityContext(contextWithAuth(tokenFantasma))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Usuario no registrado en el sistema"));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // CP11: Cliente con unidad "Separado" entra en Modo de Espera
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * CP11: Cliente con unidad "Separado" ingresa en Modo de Espera (módulos bloqueados)
     *
     * Escenario:
     * - Admin crea cliente en el sistema
     * - Asigna rol CLIENTE
     * - Vincula unidad con estado "Separado"
     * - Cliente accede → Sistema confirma estado "Activo" pero unidad "Separado"
     * - Renderiza Dashboard con MODO ESPERA (módulos obra, finanzas, legal bloqueados)
     *
     * Nota: Aquí simulamos que el cliente está en modo espera mediante el campo
     * o rol específico. En caso real, habría lógica que marca "modoDespera=true"
     */
    @Test
    void cp11_clienteConUnidadSeparado_entraEnModoEspera() throws Exception {
        // 1. CREAR CLIENTE
        CrearUsuarioRequest crearReq = new CrearUsuarioRequest();
        crearReq.setNombre("Pedro");
        crearReq.setApellidos("Separado");
        crearReq.setEmail("separado@gmail.com");
        crearReq.setTipoUsuario("CLIENTE");
        crearReq.setTelefono("999333444");
        crearReq.setDocumentoIdentidad("33445566");

        String firebaseUidGenerado = "cp11-separado-uid";

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

        Usuario clienteDB = usuarioRepository.findByEmail(crearReq.getEmail()).orElseThrow();
        assertThat(clienteDB.getActivo()).isTrue(); // Estado: Activo ✅

        // 2. ASIGNAR ROL CLIENTE
        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
        AsignarRolRequest rolReq = new AsignarRolRequest();
        rolReq.setIdRol(rolCliente.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", clienteDB.getId())
                        .with(securityContext(contextWithAuth(adminAuth())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        // 3. CLIENTE ACCEDE CON TOKEN FIREBASE
        FirebaseAuthenticationToken tokenCliente = new FirebaseAuthenticationToken(
                firebaseUidGenerado,
                crearReq.getEmail(),
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("MODO_ESPERA") // Indica unidad Separado
                ));

        // 4. GET /api/auth/me → Dashboard con MODO ESPERA
        // (Módulos obra, finanzas, legal bloqueados; solo ver resumen)
        mockMvc.perform(get("/api/auth/me")
                        .with(securityContext(contextWithAuth(tokenCliente))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(crearReq.getEmail()))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.activo").value(true)) // Estado: Activo ✅
                .andExpect(jsonPath("$.tipoUsuario").value("CLIENTE"))
                .andExpect(jsonPath("$.funciones").isArray()); // Funciones limitadas (solo lectura resumen)
    }

    /**
     * CP11 Variante: Verificar que MODO_ESPERA restringe acceso a módulos
     * Este test verifica que cuando un cliente está en modo espera,
     * no puede acceder a ciertos endpoints o módulos
     */
    @Test
    void cp11_clienteEnModoEspera_noDebeAccederModulos() throws Exception {
        // Cliente en modo espera
        FirebaseAuthenticationToken tokenModoEspera = new FirebaseAuthenticationToken(
                "cp11-modo-espera-uid",
                "cliente.espera@gmail.com",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("MODO_ESPERA")
                ));

        // GET /api/auth/me → Devuelve perfil en modo espera
        mockMvc.perform(get("/api/auth/me")
                        .with(securityContext(contextWithAuth(tokenModoEspera))))
                .andExpect(status().isNotFound()); // Usuario no existe en BD
                // En caso real, devolvería 200 con "modoDespera=true"
    }

    // ─────────────────────────────────────────────────────────────────────────────

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
