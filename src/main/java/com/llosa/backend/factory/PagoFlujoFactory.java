package com.llosa.backend.factory;

import com.llosa.backend.pagos.ConceptoPago;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.pagos.entity.Pago;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PagoFlujoFactory {

    public List<Pago> generarPagos(CronogramaPago cronograma, String tipoFinanciamiento) {
        List<Pago> pagos = new ArrayList<>();

        // 1. Pagos base (siempre se crean si existen)
        agregarPagosBase(pagos, cronograma);

        // 2. Determinar el flujo según el tipo de financiamiento
        boolean esHipotecario = tipoFinanciamiento != null
                && tipoFinanciamiento.toUpperCase().contains("HIPOT");

        if (esHipotecario) {
            generarPagoHipotecario(pagos, cronograma);
        } else {
            generarCuotasDirectas(pagos, cronograma);
        }

        return pagos;
    }

    // =========================================================================
    // MÉTODOS EXTRACTOS PARA REDUCIR COMPLEJIDAD COGNITIVA
    // =========================================================================

    private void agregarPagosBase(List<Pago> pagos, CronogramaPago cronograma) {
        if (cronograma.getPagoSeparacion() != null) {
            pagos.add(construirPago(cronograma, -1, cronograma.getPagoSeparacion(), ConceptoPago.SEPARACION));
        }
        if (cronograma.getPagoInicial() != null) {
            pagos.add(construirPago(cronograma, 0, cronograma.getPagoInicial(), ConceptoPago.INICIAL));
        }
    }

    private void generarPagoHipotecario(List<Pago> pagos, CronogramaPago cronograma) {
        BigDecimal pagado = BigDecimal.ZERO;

        if (cronograma.getPagoSeparacion() != null) {
            pagado = pagado.add(cronograma.getPagoSeparacion());
        }
        if (cronograma.getPagoInicial() != null) {
            pagado = pagado.add(cronograma.getPagoInicial());
        }

        BigDecimal completo = cronograma.getTotalPactado().subtract(pagado);

        if (completo.compareTo(BigDecimal.ZERO) > 0) {
            pagos.add(construirPago(cronograma, 1, completo, ConceptoPago.COMPLETO));
        }
    }

    private void generarCuotasDirectas(List<Pago> pagos, CronogramaPago cronograma) {
        int numCuotas = cronograma.getNumeroCuotas() != null ? cronograma.getNumeroCuotas() : 0;

        // Cláusula de guarda para evitar anidar el for dentro de un if
        if (numCuotas <= 0) {
            return;
        }

        BigDecimal montoCuota = calcularMontoCuota(cronograma, numCuotas);
        LocalDate vencimientoBase = LocalDate.now().plusMonths(1);

        for (int i = 1; i <= numCuotas; i++) {
            // CORRECCIÓN SONAR: Casteo explícito a (long) para evitar el warning aritmético
            pagos.add(construirPago(cronograma, i, montoCuota,
                    ConceptoPago.CUOTA, vencimientoBase.plusMonths((long) i - 1)));
        }
    }

    private Pago construirPago(CronogramaPago cronograma, int nroCuota,
                                BigDecimal monto, ConceptoPago concepto) {
        return construirPago(cronograma, nroCuota, monto, concepto,
                LocalDate.now().plusMonths(1));
    }

    private Pago construirPago(CronogramaPago cronograma, int nroCuota,
                                BigDecimal monto, ConceptoPago concepto,
                                LocalDate fechaVencimiento) {
        return Pago.builder()
                .cronograma(cronograma)
                .nroCuota(nroCuota)
                .montoProgramado(monto)
                .fechaVencimiento(fechaVencimiento)
                .estado("PENDIENTE")
                .concepto(concepto)
                .build();
    }

    private BigDecimal calcularMontoCuota(CronogramaPago cronograma, int numCuotas) {
        BigDecimal pendiente = cronograma.getTotalPactado();
        if (cronograma.getPagoSeparacion() != null)
            pendiente = pendiente.subtract(cronograma.getPagoSeparacion());
        if (cronograma.getPagoInicial() != null)
            pendiente = pendiente.subtract(cronograma.getPagoInicial());

        if (pendiente.compareTo(BigDecimal.ZERO) <= 0 || numCuotas <= 0) {
            return BigDecimal.ZERO;
        }
        return pendiente.divide(BigDecimal.valueOf(numCuotas), 2, RoundingMode.HALF_UP);
    }
}
