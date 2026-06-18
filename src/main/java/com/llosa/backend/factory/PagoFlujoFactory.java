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

        boolean esHipotecario = tipoFinanciamiento != null
                && tipoFinanciamiento.toUpperCase().contains("HIPOT");

        // Pago de separación
        if (cronograma.getPagoSeparacion() != null
                && cronograma.getPagoSeparacion().compareTo(BigDecimal.ZERO) > 0) {
            pagos.add(construirPago(cronograma, -1, cronograma.getPagoSeparacion(),
                    ConceptoPago.SEPARACION));
        }

        // Pago inicial
        if (cronograma.getPagoInicial() != null
                && cronograma.getPagoInicial().compareTo(BigDecimal.ZERO) > 0) {
            pagos.add(construirPago(cronograma, 0, cronograma.getPagoInicial(),
                    ConceptoPago.INICIAL));
        }

        if (esHipotecario) {
            // Pago completo (restante después de separación e inicial)
            BigDecimal pagado = BigDecimal.ZERO;
            if (cronograma.getPagoSeparacion() != null)
                pagado = pagado.add(cronograma.getPagoSeparacion());
            if (cronograma.getPagoInicial() != null)
                pagado = pagado.add(cronograma.getPagoInicial());

            BigDecimal completo = cronograma.getTotalPactado().subtract(pagado);
            if (completo.compareTo(BigDecimal.ZERO) > 0) {
                pagos.add(construirPago(cronograma, 1, completo,
                        ConceptoPago.COMPLETO));
            }
        } else {
            // Cuotas regulares para crédito directo
            int numCuotas = cronograma.getNumeroCuotas() != null ? cronograma.getNumeroCuotas() : 0;
            if (numCuotas > 0) {
                BigDecimal montoCuota = calcularMontoCuota(cronograma, numCuotas);
                LocalDate vencimientoBase = LocalDate.now().plusMonths(1);

                for (int i = 1; i <= numCuotas; i++) {
                    pagos.add(construirPago(cronograma, i, montoCuota,
                            ConceptoPago.CUOTA, vencimientoBase.plusMonths(i - 1)));
                }
            }
        }

        return pagos;
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
