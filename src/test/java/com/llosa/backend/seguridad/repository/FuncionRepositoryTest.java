package com.llosa.backend.seguridad.repository;

import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.seguridad.entity.Funcion;
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

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@Import(PostgresTestContainerConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FuncionRepositoryTest {

    @Autowired
    FuncionRepository funcionRepository;

    @Test
    void findAll_devuelve13FuncionesDelSeed() {
        List<Funcion> funciones = funcionRepository.findAll();
        assertThat(funciones).hasSize(13);
    }

    @Test
    void findByNombreCodigoIn_devuelveSoloLasSolicitadas() {
        List<Funcion> resultado = funcionRepository.findByNombreCodigoIn(
                List.of("PROY_VER", "PROY_CREAR", "DOCS_VER"));

        assertThat(resultado).hasSize(3);
        assertThat(resultado)
                .extracting("nombreCodigo")
                .containsExactlyInAnyOrder("PROY_VER", "PROY_CREAR", "DOCS_VER");
    }

    @Test
    void findByNombreCodigoIn_conListaVacia_devuelveVacio() {
        List<Funcion> resultado = funcionRepository.findByNombreCodigoIn(List.of());
        assertThat(resultado).isEmpty();
    }

    @Test
    void findByNombreCodigoIn_conCodigosInexistentes_devuelveVacio() {
        List<Funcion> resultado = funcionRepository.findByNombreCodigoIn(
                List.of("FUNCION_FALSA", "OTRA_FALSA"));
        assertThat(resultado).isEmpty();
    }

    @Test
    void todosLosCodigos_sonUnicos() {
        List<Funcion> funciones = funcionRepository.findAll();
        long distinct = funciones.stream().map(Funcion::getNombreCodigo).distinct().count();
        assertThat(distinct).isEqualTo(funciones.size());
    }
}
