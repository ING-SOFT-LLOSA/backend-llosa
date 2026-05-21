package com.llosa.backend.module.seguridad.repository;

import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.config.TestData;
import com.llosa.backend.module.seguridad.entity.Rol;
import com.llosa.backend.module.seguridad.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@Import({PostgresTestContainerConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UsuarioRepositoryTest {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    RolRepository rolRepository;

    @Autowired
    TestEntityManager em;

    // ── findByFirebaseUuid ────────────────────────────────────────────────────

    @Test
    void findByFirebaseUuid_devuelveUsuarioExistente() {
        Usuario u = TestData.usuario();
        em.persistAndFlush(u);

        Optional<Usuario> result = usuarioRepository.findByFirebaseUuid(u.getFirebaseUuid());

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(u.getEmail());
    }

    @Test
    void findByFirebaseUuid_devuelveVacioSiNoExiste() {
        Optional<Usuario> result = usuarioRepository.findByFirebaseUuid("uid-inexistente-xyz");
        assertThat(result).isEmpty();
    }

    // ── existsByEmail ─────────────────────────────────────────────────────────

    @Test
    void existsByEmail_verdaderoSiEmailRegistrado() {
        Usuario u = TestData.usuario();
        em.persistAndFlush(u);

        assertThat(usuarioRepository.existsByEmail(u.getEmail())).isTrue();
    }

    @Test
    void existsByEmail_falsoSiEmailNoRegistrado() {
        assertThat(usuarioRepository.existsByEmail("noexiste@test.com")).isFalse();
    }

    // ── Restricciones de base de datos ────────────────────────────────────────

    @Test
    void guardarDosUsuariosConMismoEmail_lanzaExcepcionUNIQUE() {
        Usuario u1 = TestData.usuarioConEmail("duplicado@test.com");
        usuarioRepository.saveAndFlush(u1);

        Usuario u2 = TestData.usuarioConEmail("duplicado@test.com");

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(u2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guardarUsuarioConTipoInvalido_lanzaExcepcionCHECK() {
        Usuario u = TestData.usuario();
        u.setTipoUsuario("OTRO");

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(u))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── Seeds de Flyway ───────────────────────────────────────────────────────

    @Test
    void migrationV1_seedsDeRolesExistentes() {
        long totalRoles = rolRepository.count();
        assertThat(totalRoles).isGreaterThanOrEqualTo(6);
    }

    @Test
    void migrationV1_rolAdminExisteConFunciones() {
        Optional<Rol> admin = rolRepository.findByNombre("ADMIN");

        assertThat(admin).isPresent();
        assertThat(admin.get().getFunciones()).isNotEmpty();
    }

    @Test
    void guardarUsuarioConRolExistente_persisteRelacion() {
        Rol rol = rolRepository.findByNombre("CLIENTE").orElseThrow();
        Usuario u = TestData.usuario();
        u.setRol(rol);
        usuarioRepository.saveAndFlush(u);

        em.clear();
        Usuario cargado = usuarioRepository.findById(u.getId()).orElseThrow();
        assertThat(cargado.getRol().getNombre()).isEqualTo("CLIENTE");
    }
}
