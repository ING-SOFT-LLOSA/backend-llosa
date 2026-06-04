package com.llosa.backend.seguridad.repository;

import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.seguridad.entity.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@Import(PostgresTestContainerConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RolRepositoryTest {

    @Autowired
    RolRepository rolRepository;

    @Autowired
    FuncionRepository funcionRepository;

    @Test
    void findByNombre_devuelveRolSeedDeFlayway() {
        Optional<Rol> rol = rolRepository.findByNombre("ADMIN");

        assertThat(rol).isPresent();
        assertThat(rol.get().getDescripcion()).contains("Administrador");
    }

    @Test
    void findByNombre_devuelveVacioParaNombreInexistente() {
        Optional<Rol> rol = rolRepository.findByNombre("ROL_INEXISTENTE");
        assertThat(rol).isEmpty();
    }

    @Test
    void findAll_devuelveSeisSeedsMinimo() {
        List<Rol> roles = rolRepository.findAll();
        assertThat(roles).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void rolCliente_tieneFuncionesAsignadas() {
        Rol cliente = rolRepository.findByNombre("CLIENTE").orElseThrow();

        assertThat(cliente.getFunciones())
                .extracting("nombreCodigo")
                .containsExactlyInAnyOrder(
                        "PROY_VER", "DOCS_VER", "PAGOS_VER", "OBRA_VER", "CONTRATO_VER");
    }

    @Test
    void rolAdmin_tieneTodasFunciones() {
        Rol admin = rolRepository.findByNombre("ADMIN").orElseThrow();
        long totalFunciones = funcionRepository.count();
 
        assertThat(admin.getFunciones()).hasSize((int) totalFunciones);
    }
 
    @Test
    void rolLegal_tieneFuncionesCorrectas() {
        Rol legal = rolRepository.findByNombre("LEGAL").orElseThrow();
 
        assertThat(legal.getFunciones())
                .extracting("nombreCodigo")
                .containsExactlyInAnyOrder("CONTRATO_VER", "CONTRATO_EDITAR", "DOCS_VER");
    }
 
    @Test
    void rolAsesor_tieneFuncionesCorrectas() {
        Rol asesor = rolRepository.findByNombre("ASESOR").orElseThrow();
 
        assertThat(asesor.getFunciones())
                .extracting("nombreCodigo")
                .containsExactlyInAnyOrder(
                        "PROY_VER", "USER_GESTIONAR", "DOCS_VER", "DOCS_SUBIR", "PAGOS_VER", "CONTRATO_VER", "USER_VER");
    }

    @Test
    void rolTecnico_tieneFuncionesCorrectas() {
        Rol tecnico = rolRepository.findByNombre("TECNICO").orElseThrow();

        assertThat(tecnico.getFunciones())
                .extracting("nombreCodigo")
                .containsExactlyInAnyOrder("OBRA_VER", "OBRA_EDITAR", "PROY_VER");
    }

    @Test
    void rolPostventa_tieneFuncionesCorrectas() {
        Rol postventa = rolRepository.findByNombre("POSTVENTA").orElseThrow();

        assertThat(postventa.getFunciones())
                .extracting("nombreCodigo")
                .containsExactlyInAnyOrder("PROY_VER", "DOCS_VER", "PAGOS_VER");
    }
}
