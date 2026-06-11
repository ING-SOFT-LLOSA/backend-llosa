package com.llosa.backend.pagos.repository;

import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.pagos.entity.CartaAprobacion;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CartaAprobacionRepositoryTest {

    @Autowired
    CartaAprobacionRepository cartaAprobacionRepository;

    @Autowired
    TestEntityManager em;

    private UsuarioActivo ua;

    @BeforeEach
    void setUp() {
        Proyecto proyecto = Proyecto.builder().nombre("Test Proyecto").build();
        em.persist(proyecto);

        Torre torre = Torre.builder().nombre("Torre A").proyecto(proyecto).build();
        em.persist(torre);

        Piso piso = Piso.builder().nroPiso(1).torre(torre).build();
        em.persist(piso);

        Activo activo = Activo.builder()
                .nro("A-101")
                .tipo(TipoActivo.DEPARTAMENTO)
                .estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(piso)
                .build();
        em.persist(activo);

        ua = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .activos(List.of(activo))
                .build();
        em.persist(ua);
        em.flush();
    }

    @Test
    void findByUsuarioActivo_UuidUsuarioActivo_devuelveCarta() {
        CartaAprobacion ca = CartaAprobacion.builder()
                .usuarioActivo(ua)
                .banco("Banco de Prueba")
                .montoAprobado(new BigDecimal("300000.00"))
                .fechaEmision(LocalDate.now())
                .build();
        em.persistAndFlush(ca);

        Optional<CartaAprobacion> result = cartaAprobacionRepository
                .findByUsuarioActivo_UuidUsuarioActivo(ua.getUuidUsuarioActivo());

        assertThat(result).isPresent();
        assertThat(result.get().getBanco()).isEqualTo("Banco de Prueba");
    }

    @Test
    void findByUsuarioActivo_UuidUsuarioActivo_vacioSiNoExiste() {
        Optional<CartaAprobacion> result = cartaAprobacionRepository
                .findByUsuarioActivo_UuidUsuarioActivo(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void existsByUsuarioActivo_UuidUsuarioActivo_verdaderoSiExiste() {
        CartaAprobacion ca = CartaAprobacion.builder()
                .usuarioActivo(ua)
                .banco("Banco de Prueba")
                .montoAprobado(new BigDecimal("300000.00"))
                .build();
        em.persistAndFlush(ca);

        assertThat(cartaAprobacionRepository.existsByUsuarioActivo_UuidUsuarioActivo(ua.getUuidUsuarioActivo()))
                .isTrue();
    }

    @Test
    void existsByUsuarioActivo_UuidUsuarioActivo_falsoSiNoExiste() {
        assertThat(cartaAprobacionRepository.existsByUsuarioActivo_UuidUsuarioActivo(UUID.randomUUID()))
                .isFalse();
    }
}
