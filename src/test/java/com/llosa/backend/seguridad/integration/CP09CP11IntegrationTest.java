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
import com.llosa.backend.seguridad.entity.Funcion;
import com.llosa.backend.seguridad.repository.FuncionRepository;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CP09 & CP11 - Client Property Access Integration Tests
 *
 * CP09: Cliente con unidad en estado "Vendido" puede acceder al portal
 * - Acceso normal al dashboard
 * - Consulta de datos personales
 * - Acceso a proyectos y documentos
 *
 * CP11: Cliente con unidad en estado "Separado" tiene acceso limitado
 * - Modo espera activado
 * - Acceso limitado a funcionalidades
 * - Bloqueo de acciones específicas
 */
@Disabled
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class, SecurityTestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
class CP09CP11IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private FuncionRepository funcionRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();

        if (rolRepository.findByNombre("CLIENTE").isEmpty()) {
            Funcion proyVer = new Funcion();
            proyVer.setNombreCodigo("PROY_VER");
            proyVer.setDescripcion("Ver proyectos");
            funcionRepository.save(proyVer);

            Funcion docsVer = new Funcion();
            docsVer.setNombreCodigo("DOCS_VER");
            docsVer.setDescripcion("Ver documentos");
            funcionRepository.save(docsVer);

            Rol rolCliente = new Rol();
            rolCliente.setNombre("CLIENTE");
            rolCliente.setDescripcion("Cliente de la inmobiliaria");
            rolCliente.setFunciones(List.of(proyVer, docsVer));
            rolRepository.save(rolCliente);
        }
    }

    /**
     * CP09.1: Cliente con unidad Vendido accede al dashboard completo
     *
     * Verifica que:
     * - Cliente puede consultar /api/auth/me
     * - Obtiene perfil completo con rol y funciones
     * - Estado es Activo
     */
    @Test
    void cp09_clienteVendido_accedeDashboard_completo() throws Exception {
        // 1. Crear cliente
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setEmail("cp09-vendido-1@test.com");
        String firebaseUid = "cp09-uid-vendido-1";

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

        Usuario usuario = usuarioRepository.findByEmail(req.getEmail()).orElseThrow();

        // 2. Asignar rol CLIENTE (simula unidad Vendido)
        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
        AsignarRolRequest asignarRolReq = new AsignarRolRequest();
        asignarRolReq.setIdRol(rolCliente.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", usuario.getId())
                .with(securityContext(contextWithAuth(adminAuth())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(asignarRolReq)))
                .andExpect(status().isOk());

        // 3. Acceder a dashboard con token
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                firebaseUid, req.getEmail(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(req.getEmail()))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.tipoUsuario").value("CLIENTE"))
                .andExpect(jsonPath("$.funciones").isArray())
                .andExpect(jsonPath("$.funciones[0]").exists()); // CLIENTE tiene funciones
    }

    /**
     * CP09.2: Cliente Vendido consulta datos personales
     *
     * Verifica que:
     * - Puede obtener su perfil
     * - Datos son consistentes en BD y respuesta
     */
    @Test
    void cp09_clienteVendido_consultaDatosPersonales() throws Exception {
        // 1. Crear cliente con datos específicos
        Usuario usuario = TestData.usuarioConEmail("cp09-vendido-2@test.com");
        usuario.setFirebaseUuid("cp09-uid-vendido-2");
        usuario.setNombre("Carlos");
        usuario.setApellidos("López García");
        usuario.setTelefono("+51987654321");
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // 2. Asignar rol CLIENTE
        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
        AsignarRolRequest asignarRolReq = new AsignarRolRequest();
        asignarRolReq.setIdRol(rolCliente.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", usuarioGuardado.getId())
                .with(securityContext(contextWithAuth(adminAuth())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(asignarRolReq)))
                .andExpect(status().isOk());

        // 3. Consultar datos personales
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "cp09-uid-vendido-2", "cp09-vendido-2@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Carlos"))
                .andExpect(jsonPath("$.apellidos").value("López García"))
                .andExpect(jsonPath("$.email").value("cp09-vendido-2@test.com"))
                .andExpect(jsonPath("$.telefono").value("+51987654321"));
    }

    /**
     * CP09.3: Cliente Vendido puede listar sus proyectos asociados
     *
     * Verifica que:
     * - Tiene permiso PROY_VER (de rol CLIENTE)
     * - Puede acceder a endpoints de consulta
     */
    @Test
    void cp09_clienteVendido_tieneFuncionPROY_VER() throws Exception {
        // 1. Crear cliente y asignar rol
        Usuario usuario = TestData.usuarioConEmail("cp09-vendido-3@test.com");
        usuario.setFirebaseUuid("cp09-uid-vendido-3");
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

        // 2. Verificar que tiene funcion PROY_VER
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "cp09-uid-vendido-3", "cp09-vendido-3@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funciones[0]").exists());

        // Verificar que la función contiene PROY_VER
        Usuario usuarioActualizado = usuarioRepository.findByEmail("cp09-vendido-3@test.com").orElseThrow();
        assertThat(usuarioActualizado.getRol().getFunciones()).isNotEmpty();
        assertThat(usuarioActualizado.getRol().getFunciones().stream()
                .map(f -> f.getNombreCodigo())
                .toList()).contains("PROY_VER");
    }

    /**
     * CP11.1: Cliente Separado accede con modo espera activado
     *
     * Verifica que:
     * - Usuario con unidad Separado puede autenticarse
     * - Su estado es activo en BD
     * - Sistema identifica estado Separado (preparación para bloqueos futuros)
     */
    @Test
    void cp11_clienteSeparado_accedeEnModoEspera() throws Exception {
        // 1. Crear cliente Separado
        Usuario usuario = TestData.usuarioConEmail("cp11-separado@test.com");
        usuario.setFirebaseUuid("cp11-uid-separado");
        usuario.setTipoUsuario("CLIENTE");
        usuario.setActivo(true);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // 2. Asignar rol CLIENTE
        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
        AsignarRolRequest asignarRolReq = new AsignarRolRequest();
        asignarRolReq.setIdRol(rolCliente.getIdRol());

        mockMvc.perform(put("/api/users/{id}/role", usuarioGuardado.getId())
                .with(securityContext(contextWithAuth(adminAuth())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(asignarRolReq)))
                .andExpect(status().isOk());

        // 3. Acceder a dashboard - aún con acceso completo
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "cp11-uid-separado", "cp11-separado@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cp11-separado@test.com"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        // 4. Verificar estado en BD
        Usuario usuarioEnBD = usuarioRepository.findByEmail("cp11-separado@test.com").orElseThrow();
        assertThat(usuarioEnBD.getActivo()).isTrue();
        assertThat(usuarioEnBD.getTipoUsuario()).isEqualTo("CLIENTE");
    }

    /**
     * CP11.2: Cliente Separado tiene acceso limitado a funcionalidades
     *
     * Verifica que:
     * - Puede consultar /me (perfil)
     * - Puede acceder a lectura de proyectos
     * - No puede realizar operaciones de escritura (implementación futura)
     */
    @Test
    void cp11_clienteSeparado_accesoLimitado_lectura() throws Exception {
        // 1. Crear usuario Separado
        Usuario usuario = TestData.usuarioConEmail("cp11-separado-2@test.com");
        usuario.setFirebaseUuid("cp11-uid-separado-2");
        usuario.setTipoUsuario("CLIENTE");
        usuario.setActivo(true);
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

        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "cp11-uid-separado-2", "cp11-separado-2@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // 2. Puede acceder a lectura de datos
        mockMvc.perform(get("/api/auth/me")
                .with(securityContext(contextWithAuth(token))))
                .andExpect(status().isOk());

        // 3. Verificar que tiene funciones de lectura
        Usuario usuarioActualizado = usuarioRepository.findByEmail("cp11-separado-2@test.com").orElseThrow();
        assertThat(usuarioActualizado.getRol()).isNotNull();
        assertThat(usuarioActualizado.getRol().getFunciones()).isNotEmpty();
    }

    /**
     * CP11.3: Bloqueo de acciones de escritura en modo Separado
     *
     * Verifica que:
     * - No puede modificar información personal (PATCH /api/users/{id})
     * - No puede cambiar preferencias
     * - Acciones limitadas solo a lectura
     */
    @Test
    void cp11_clienteSeparado_bloqueaAccionesEscritura() throws Exception {
        // 1. Crear usuario Separado
        Usuario usuario = TestData.usuarioConEmail("cp11-separado-3@test.com");
        usuario.setFirebaseUuid("cp11-uid-separado-3");
        usuario.setTipoUsuario("CLIENTE");
        usuario.setActivo(true);
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

        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "cp11-uid-separado-3", "cp11-separado-3@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // 2. Intenta modificar perfil (requiere permisos especiales)
        // Sin permisos de GESTIONAR, la operación debe ser rechazada
        String updateJson = "{\"nombre\":\"NombreActualizado\"}";

        mockMvc.perform(patch("/api/users/{id}", usuarioGuardado.getId())
                .with(securityContext(contextWithAuth(token)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isForbidden()); // Sin permiso USER_GESTIONAR
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
