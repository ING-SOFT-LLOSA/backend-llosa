package com.llosa.backend.factory;

import com.llosa.backend.pagos.ConceptoPago;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.pagos.entity.Pago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PagoFlujoFactoryTest {

    private final PagoFlujoFactory factory = new PagoFlujoFactory();

    private CronogramaPago.CronogramaPagoBuilder baseCronograma() {
        return CronogramaPago.builder()
                .totalPactado(new BigDecimal("100000.00"));
    }

    // ── Pagos base ───────────────────────────────────────────────────────────

    @Test
    void generarPagos_sinSeparacionNiInicial_noAgregaPagosBase() {
        CronogramaPago cronograma = baseCronograma()
                .numeroCuotas(0)
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "DIRECTO");

        assertThat(pagos).noneMatch(p -> p.getConcepto() == ConceptoPago.SEPARACION);
        assertThat(pagos).noneMatch(p -> p.getConcepto() == ConceptoPago.INICIAL);
    }

    @Test
    void generarPagos_conSeparacionEInicial_agregaAmbosPagosBase() {
        CronogramaPago cronograma = baseCronograma()
                .pagoSeparacion(new BigDecimal("1000.00"))
                .pagoInicial(new BigDecimal("9000.00"))
                .numeroCuotas(0)
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "DIRECTO");

        Pago separacion = pagos.stream().filter(p -> p.getConcepto() == ConceptoPago.SEPARACION).findFirst().orElseThrow();
        assertThat(separacion.getNroCuota()).isEqualTo(-1);
        assertThat(separacion.getMontoProgramado()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(separacion.getEstado()).isEqualTo("PENDIENTE");
        assertThat(separacion.getCronograma()).isSameAs(cronograma);

        Pago inicial = pagos.stream().filter(p -> p.getConcepto() == ConceptoPago.INICIAL).findFirst().orElseThrow();
        assertThat(inicial.getNroCuota()).isZero();
        assertThat(inicial.getMontoProgramado()).isEqualTo(new BigDecimal("9000.00"));
    }

    // ── Flujo hipotecario ────────────────────────────────────────────────────

    @Test
    void generarPagos_hipotecario_generaPagoCompletoConSaldoRestante() {
        CronogramaPago cronograma = baseCronograma()
                .pagoSeparacion(new BigDecimal("1000.00"))
                .pagoInicial(new BigDecimal("9000.00"))
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "CREDITO HIPOTECARIO");

        Pago completo = pagos.stream().filter(p -> p.getConcepto() == ConceptoPago.COMPLETO).findFirst().orElseThrow();
        assertThat(completo.getNroCuota()).isEqualTo(1);
        assertThat(completo.getMontoProgramado()).isEqualTo(new BigDecimal("90000.00"));
    }

    @Test
    void generarPagos_hipotecario_esCaseInsensitive() {
        CronogramaPago cronograma = baseCronograma().build();

        List<Pago> pagos = factory.generarPagos(cronograma, "credito hipotecario");

        assertThat(pagos).anyMatch(p -> p.getConcepto() == ConceptoPago.COMPLETO);
    }

    @Test
    void generarPagos_hipotecario_sinPagosPreviosUsaTotalPactadoCompleto() {
        CronogramaPago cronograma = baseCronograma().build();

        List<Pago> pagos = factory.generarPagos(cronograma, "HIPOTECARIO");

        Pago completo = pagos.stream().filter(p -> p.getConcepto() == ConceptoPago.COMPLETO).findFirst().orElseThrow();
        assertThat(completo.getMontoProgramado()).isEqualTo(new BigDecimal("100000.00"));
    }

    @Test
    void generarPagos_hipotecario_saldoCero_noAgregaPagoCompleto() {
        CronogramaPago cronograma = baseCronograma()
                .pagoSeparacion(new BigDecimal("50000.00"))
                .pagoInicial(new BigDecimal("50000.00"))
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "HIPOTECARIO");

        assertThat(pagos).noneMatch(p -> p.getConcepto() == ConceptoPago.COMPLETO);
    }

    @Test
    void generarPagos_hipotecario_saldoNegativo_noAgregaPagoCompleto() {
        CronogramaPago cronograma = baseCronograma()
                .pagoSeparacion(new BigDecimal("60000.00"))
                .pagoInicial(new BigDecimal("60000.00"))
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "HIPOTECARIO");

        assertThat(pagos).noneMatch(p -> p.getConcepto() == ConceptoPago.COMPLETO);
    }

    @Test
    void generarPagos_tipoFinanciamientoNull_usaFlujoDirecto() {
        CronogramaPago cronograma = baseCronograma()
                .numeroCuotas(2)
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, null);

        assertThat(pagos).noneMatch(p -> p.getConcepto() == ConceptoPago.COMPLETO);
        assertThat(pagos).filteredOn(p -> p.getConcepto() == ConceptoPago.CUOTA).hasSize(2);
    }

    // ── Flujo de cuotas directas ─────────────────────────────────────────────

    @Test
    void generarPagos_directo_numeroCuotasNull_noGeneraCuotas() {
        CronogramaPago cronograma = baseCronograma()
                .numeroCuotas(null)
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "DIRECTO");

        assertThat(pagos).noneMatch(p -> p.getConcepto() == ConceptoPago.CUOTA);
    }

    @Test
    void generarPagos_directo_numeroCuotasCero_noGeneraCuotas() {
        CronogramaPago cronograma = baseCronograma()
                .numeroCuotas(0)
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "DIRECTO");

        assertThat(pagos).noneMatch(p -> p.getConcepto() == ConceptoPago.CUOTA);
    }

    @Test
    void generarPagos_directo_numeroCuotasNegativo_noGeneraCuotas() {
        CronogramaPago cronograma = baseCronograma()
                .numeroCuotas(-3)
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "DIRECTO");

        assertThat(pagos).noneMatch(p -> p.getConcepto() == ConceptoPago.CUOTA);
    }

    @Test
    void generarPagos_directo_generaCuotasConMontoYFechasCorrectas() {
        CronogramaPago cronograma = baseCronograma()
                .pagoSeparacion(new BigDecimal("10000.00"))
                .pagoInicial(new BigDecimal("10000.00"))
                .numeroCuotas(4)
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "DIRECTO");

        List<Pago> cuotas = pagos.stream().filter(p -> p.getConcepto() == ConceptoPago.CUOTA).toList();
        assertThat(cuotas).hasSize(4);

        BigDecimal montoEsperado = new BigDecimal("80000.00").divide(BigDecimal.valueOf(4), 2, java.math.RoundingMode.HALF_UP);
        LocalDate vencimientoBase = LocalDate.now().plusMonths(1);

        for (int i = 0; i < cuotas.size(); i++) {
            Pago cuota = cuotas.get(i);
            assertThat(cuota.getNroCuota()).isEqualTo(i + 1);
            assertThat(cuota.getMontoProgramado()).isEqualTo(montoEsperado);
            assertThat(cuota.getFechaVencimiento()).isEqualTo(vencimientoBase.plusMonths(i));
            assertThat(cuota.getEstado()).isEqualTo("PENDIENTE");
        }
    }

    @Test
    void generarPagos_directo_pendienteCeroOMenor_montoCuotaEsCero() {
        CronogramaPago cronograma = baseCronograma()
                .pagoSeparacion(new BigDecimal("60000.00"))
                .pagoInicial(new BigDecimal("60000.00"))
                .numeroCuotas(3)
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "DIRECTO");

        List<Pago> cuotas = pagos.stream().filter(p -> p.getConcepto() == ConceptoPago.CUOTA).toList();
        assertThat(cuotas).hasSize(3);
        assertThat(cuotas).allMatch(c -> c.getMontoProgramado().compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void generarPagos_directo_sinPagosPrevios_calculaSobreTotalPactado() {
        CronogramaPago cronograma = baseCronograma()
                .numeroCuotas(5)
                .build();

        List<Pago> pagos = factory.generarPagos(cronograma, "DIRECTO");

        List<Pago> cuotas = pagos.stream().filter(p -> p.getConcepto() == ConceptoPago.CUOTA).toList();
        BigDecimal montoEsperado = new BigDecimal("100000.00").divide(BigDecimal.valueOf(5), 2, java.math.RoundingMode.HALF_UP);
        assertThat(cuotas).allMatch(c -> c.getMontoProgramado().compareTo(montoEsperado) == 0);
    }
}
