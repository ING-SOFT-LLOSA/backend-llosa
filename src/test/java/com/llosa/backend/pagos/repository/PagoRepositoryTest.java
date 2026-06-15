package com.llosa.backend.pagos.repository;

import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.pagos.entity.Pago;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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

@DataJpaTest
@Tag("integration")
@Testcontainers
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PagoRepositoryTest {

    @Autowired
    PagoRepository pagoRepository;

    @Autowired
    TestEntityManager em;

    private CronogramaPago cp;

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

        UsuarioActivo ua = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .activos(List.of(activo))
                .build();
        em.persist(ua);

        cp = CronogramaPago.builder()
                .usuarioActivo(ua)
                .totalPactado(new BigDecimal("100000.00"))
                .numeroCuotas(4)
                .estado("ACTIVO")
                .build();
        em.persist(cp);
        em.flush();
    }

    @Test
    void findByCronograma_IdOrderByNroCuotaAsc_devuelveOrdenados() {
        em.persist(Pago.builder().cronograma(cp).nroCuota(2)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(2))
                .estado("PENDIENTE").build());
        em.persist(Pago.builder().cronograma(cp).nroCuota(1)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(1))
                .estado("PENDIENTE").build());
        em.flush();

        List<Pago> pagos = pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(cp.getId());

        assertThat(pagos).hasSize(2);
        assertThat(pagos.get(0).getNroCuota()).isEqualTo(1);
        assertThat(pagos.get(1).getNroCuota()).isEqualTo(2);
    }

    @Test
    void findByCronograma_IdOrderByNroCuotaAsc_vacioSiNoHayPagos() {
        List<Pago> pagos = pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(cp.getId());

        assertThat(pagos).isEmpty();
    }

    @Test
    void findByCronograma_IdAndNroCuota_devuelveCuotaEspecifica() {
        em.persist(Pago.builder().cronograma(cp).nroCuota(3)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(3))
                .estado("PENDIENTE").build());
        em.flush();

        Optional<Pago> result = pagoRepository.findByCronograma_IdAndNroCuota(cp.getId(), 3);

        assertThat(result).isPresent();
        assertThat(result.get().getNroCuota()).isEqualTo(3);
    }

    @Test
    void findByCronograma_IdAndNroCuota_vacioSiNoExiste() {
        Optional<Pago> result = pagoRepository.findByCronograma_IdAndNroCuota(cp.getId(), 99);

        assertThat(result).isEmpty();
    }

    @Test
    void sumMontoPagadoByCronogramaId_sumaCorrectamente() {
        em.persist(Pago.builder().cronograma(cp).nroCuota(1)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(1))
                .estado("PAGADO").montoPagado(new BigDecimal("25000.00")).build());
        em.persist(Pago.builder().cronograma(cp).nroCuota(2)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(2))
                .estado("PAGADO").montoPagado(new BigDecimal("25000.00")).build());
        em.persist(Pago.builder().cronograma(cp).nroCuota(3)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(3))
                .estado("PENDIENTE").build());
        em.flush();

        BigDecimal totalPagado = pagoRepository.sumMontoPagadoByCronogramaId(cp.getId());

        assertThat(totalPagado).isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    void sumMontoPagadoByCronogramaId_sinPagos_devuelveCero() {
        BigDecimal totalPagado = pagoRepository.sumMontoPagadoByCronogramaId(cp.getId());

        assertThat(totalPagado).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void countByCronograma_IdAndEstado_cuentaCorrectamente() {
        em.persist(Pago.builder().cronograma(cp).nroCuota(1)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(1))
                .estado("PAGADO").montoPagado(new BigDecimal("25000.00")).build());
        em.persist(Pago.builder().cronograma(cp).nroCuota(2)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(2))
                .estado("PAGADO").montoPagado(new BigDecimal("25000.00")).build());
        em.persist(Pago.builder().cronograma(cp).nroCuota(3)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(3))
                .estado("PENDIENTE").build());
        em.flush();

        assertThat(pagoRepository.countByCronograma_IdAndEstado(cp.getId(), "PAGADO")).isEqualTo(2);
        assertThat(pagoRepository.countByCronograma_IdAndEstado(cp.getId(), "PENDIENTE")).isEqualTo(1);
        assertThat(pagoRepository.countByCronograma_IdAndEstado(cp.getId(), "VENCIDO")).isZero();
    }
}
