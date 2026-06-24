package com.llosa.backend.comercial.integration;

import com.llosa.backend.annotation.CP;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.proyecto.service.UsuarioActivoService;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.RolRepository;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import com.llosa.backend.seguridad.service.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SPRINT 10/11 — Gobernanza de Admin (CP55), Asesores (CP56) y Co-titularidad (CP57).
 * Casos de prueba del Plan de Pruebas v3 (rev. 19/06/26).
 *
 * Tests E2E de integración a nivel de servicio (contexto Spring completo sobre H2,
 * SIN Docker), @Tag("integration"). Afirman lo que el sistema DEBE cumplir según el
 * Plan; si el backend no lo cumple, el test falla (rojo) => defecto para Mantis.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Import({SecurityTestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CP55ToCP57E2ETest {

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @Autowired UsuarioService usuarioService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired RolRepository rolRepository;
    @Autowired UsuarioActivoService usuarioActivoService;
    @Autowired UsuarioActivoRepository usuarioActivoRepository;

    @BeforeEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP55: Gobernanza de administradores. El PDF exige que el sistema impida la
    //   auto-desactivación / dejar el sistema sin administrador. Caso verificable
    //   en backend: NO se puede desactivar al ÚNICO administrador del sistema.
    //   Esperado: rechazo con error explícito (BusinessException -> 400).
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP55",
        scenario = "Impedir desactivar al unico administrador del sistema",
        input = "cambiarEstado(adminId, activo=false) con un solo ADMIN activo",
        expected = "BusinessException: 'No se puede desactivar al unico administrador'",
        type = CP.TestType.E2E)
    void cp55_noDesactivarUnicoAdmin_esRechazado() {
        Rol rolAdmin = obtenerOcrearRol("ADMIN");
        Usuario admin = nuevoUsuario("gov-admin-uid", "admin.gov@llosaedificaciones.com", rolAdmin);

        // Dejar EXACTAMENTE un administrador activo: desactivar en BD cualquier otro
        // admin sembrado (data-test.sql) para aislar el escenario "unico admin".
        usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() != null && "ADMIN".equals(u.getRol().getNombre()))
                .filter(u -> !u.getId().equals(admin.getId()))
                .forEach(u -> { u.setActivo(false); usuarioRepository.save(u); });

        long adminsActivos = usuarioRepository.countByRol_NombreAndActivoTrue("ADMIN");
        assertThat(adminsActivos).isEqualTo(1);

        // La regla de negocio se evalua ANTES de tocar Firebase: el bloqueo del unico
        // admin lanza BusinessException sin llegar a revocar tokens.
        assertThatThrownBy(() -> usuarioService.cambiarEstado(admin.getId(), false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("administrador");

        // El admin debe permanecer ACTIVO tras el intento bloqueado.
        Usuario recargado = usuarioRepository.findById(admin.getId()).orElseThrow();
        assertThat(recargado.getActivo()).isTrue();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP56: Asignación y desasignación de asesores comerciales a contratos.
    //   El PDF exige: registrar el vínculo; desasignar; y que asignar a un contrato
    //   INEXISTENTE retorne error de "contrato no encontrado".
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP56",
        scenario = "Asignar y desasignar un asesor comercial a un contrato",
        input = "asignarAsesorAContrato(uuidExpediente, idAsesor) + desasignar",
        expected = "vinculo registrado; luego asesor = null",
        type = CP.TestType.E2E)
    void cp56_asignarYDesasignarAsesor_funciona() {
        Rol rolAsesor = obtenerOcrearRol("ASESOR");
        Usuario asesor = nuevoUsuario("asesor-uid", "carlos.ruiz@llosaedificaciones.com", rolAsesor);
        UsuarioActivo expediente = nuevoExpediente();

        UsuarioActivo conAsesor =
                usuarioActivoService.asignarAsesorAContrato(expediente.getUuidUsuarioActivo(), asesor.getId());
        assertThat(conAsesor.getAsesor()).isNotNull();
        assertThat(conAsesor.getAsesor().getId()).isEqualTo(asesor.getId());

        UsuarioActivo sinAsesor =
                usuarioActivoService.desasignarAsesorDelContrato(expediente.getUuidUsuarioActivo(), asesor.getId());
        assertThat(sinAsesor.getAsesor()).isNull();
    }

    @Test
    @CP(value = "CP56",
        scenario = "Asignar asesor a un contrato inexistente retorna error",
        input = "asignarAsesorAContrato(uuid aleatorio, idAsesor)",
        expected = "EntityNotFoundException (contrato no encontrado)",
        type = CP.TestType.E2E)
    void cp56_asignarAsesorAContratoInexistente_esRechazado() {
        Rol rolAsesor = obtenerOcrearRol("ASESOR");
        Usuario asesor = nuevoUsuario("asesor-uid-2", "asesor2@llosaedificaciones.com", rolAsesor);

        assertThatThrownBy(() ->
                usuarioActivoService.asignarAsesorAContrato(UUID.randomUUID(), asesor.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP57: Co-titularidad. El PDF exige vincular un titular principal y luego un
    //   segundo cliente (sociedad conyugal) al MISMO contrato. El modelo soporta
    //   múltiples clientes vía @ManyToMany usuario_activo_clientes ("esposos, socios").
    //   Se valida que ambos co-titulares queden persistidos en el mismo expediente.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @org.springframework.transaction.annotation.Transactional // mantiene la sesion abierta (clientes es LAZY)
    @CP(value = "CP57",
        scenario = "Registrar dos co-titulares en el mismo contrato",
        input = "clientes = [Juan Perez, Ana Perez] en un UsuarioActivo",
        expected = "el expediente persiste ambos co-titulares",
        type = CP.TestType.E2E)
    void cp57_coTitularidad_dosClientesMismoContrato() {
        Rol rolCliente = obtenerOcrearRol("CLIENTE");
        Usuario titular = nuevoUsuario("titular-uid", "juan.perez@gmail.com", rolCliente);
        Usuario coTitular = nuevoUsuario("cotitular-uid", "ana.perez@gmail.com", rolCliente);

        UsuarioActivo expediente = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .clientes(new ArrayList<>(List.of(titular, coTitular)))
                .build();
        usuarioActivoRepository.save(expediente);

        UsuarioActivo recargado =
                usuarioActivoRepository.findById(expediente.getUuidUsuarioActivo()).orElseThrow();
        assertThat(recargado.getClientes())
                .extracting(Usuario::getEmail)
                .containsExactlyInAnyOrder("juan.perez@gmail.com", "ana.perez@gmail.com");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Rol obtenerOcrearRol(String nombre) {
        return rolRepository.findByNombre(nombre).orElseGet(() -> {
            Rol r = new Rol();
            r.setNombre(nombre);
            r.setDescripcion("Rol " + nombre);
            return rolRepository.save(r);
        });
    }

    private Usuario nuevoUsuario(String uid, String email, Rol rol) {
        Usuario u = new Usuario();
        u.setFirebaseUuid(uid + "-" + UUID.randomUUID());
        u.setNombre("Test");
        u.setApellidos("User");
        u.setEmail(email);
        u.setTipoUsuario("EMPLEADO");
        u.setActivo(true);
        u.setRol(rol);
        u.setCreatedAt(LocalDateTime.now());
        return usuarioRepository.save(u);
    }

    private UsuarioActivo nuevoExpediente() {
        UsuarioActivo expediente = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .build();
        return usuarioActivoRepository.save(expediente);
    }

    @SuppressWarnings("unused")
    private static SecurityContext authContext() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken("test-uid", null, List.of()));
        return ctx;
    }
}
