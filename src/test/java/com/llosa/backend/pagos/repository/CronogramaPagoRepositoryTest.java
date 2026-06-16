package com.llosa.backend.pagos.repository;

import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CronogramaPagoRepositoryTest {

    @Autowired
    CronogramaPagoRepository cronogramaPagoRepository;

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
    void findByUsuarioActivo_UuidUsuarioActivo_devuelveCronograma() {
        CronogramaPago cp = CronogramaPago.builder()
                .usuarioActivo(ua)
                .totalPactado(new BigDecimal("350000.00"))
                .estado("ACTIVO")
                .build();
        em.persistAndFlush(cp);

        Optional<CronogramaPago> result = cronogramaPagoRepository
                .findByUsuarioActivo_UuidUsuarioActivo(ua.getUuidUsuarioActivo());

        assertThat(result).isPresent();
        assertThat(result.get().getTotalPactado()).isEqualByComparingTo(new BigDecimal("350000.00"));
        assertThat(result.get().getEstado()).isEqualTo("ACTIVO");
    }

    @Test
    void findByUsuarioActivo_UuidUsuarioActivo_vacioSiNoExiste() {
        Optional<CronogramaPago> result = cronogramaPagoRepository
                .findByUsuarioActivo_UuidUsuarioActivo(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void existsByUsuarioActivo_UuidUsuarioActivo_verdaderoSiExiste() {
        CronogramaPago cp = CronogramaPago.builder()
                .usuarioActivo(ua)
                .totalPactado(new BigDecimal("350000.00"))
                .estado("ACTIVO")
                .build();
        em.persistAndFlush(cp);

        assertThat(cronogramaPagoRepository.existsByUsuarioActivo_UuidUsuarioActivo(ua.getUuidUsuarioActivo()))
                .isTrue();
    }

    @Test
    void existsByUsuarioActivo_UuidUsuarioActivo_falsoSiNoExiste() {
        assertThat(cronogramaPagoRepository.existsByUsuarioActivo_UuidUsuarioActivo(UUID.randomUUID()))
                .isFalse();
    }

    @Test
    void eliminarCronograma_eliminaEnCascadaPagos() {
        CronogramaPago cp = CronogramaPago.builder()
                .usuarioActivo(ua)
                .totalPactado(new BigDecimal("350000.00"))
                .estado("ACTIVO")
                .build();
        em.persist(cp);
        em.flush();

        cronogramaPagoRepository.delete(cp);
        em.flush();

        assertThat(cronogramaPagoRepository.existsByUsuarioActivo_UuidUsuarioActivo(ua.getUuidUsuarioActivo()))
                .isFalse();
    }
}
