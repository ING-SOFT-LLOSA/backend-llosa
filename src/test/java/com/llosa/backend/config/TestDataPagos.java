package com.llosa.backend.config;

import com.llosa.backend.pagos.dto.CartaAprobacionRequest;
import com.llosa.backend.pagos.dto.CronogramaPagoRequest;
import com.llosa.backend.pagos.dto.PagoRequest;
import com.llosa.backend.pagos.entity.CartaAprobacion;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.pagos.entity.Pago;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class TestDataPagos {

    private TestDataPagos() {}

    public static Proyecto proyecto() {
        return Proyecto.builder()
                .nombre("Test Proyecto")
                .build();
    }

    public static Torre torre(Proyecto proyecto) {
        return Torre.builder()
                .nombre("Torre A")
                .proyecto(proyecto)
                .build();
    }

    public static Piso piso(Torre torre) {
        return Piso.builder()
                .nroPiso(1)
                .torre(torre)
                .build();
    }

    public static Activo activo(Piso piso) {
        return Activo.builder()
                .nro("A-101")
                .tipo(TipoActivo.DEPARTAMENTO)
                .estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(piso)
                .build();
    }

    public static UsuarioActivo usuarioActivo(Activo activo) {
        return UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .activos(List.of(activo))
                .build();
    }

    public static CronogramaPago cronogramaPago(UsuarioActivo ua) {
        return CronogramaPago.builder()
                .usuarioActivo(ua)
                .totalPactado(new BigDecimal("350000.00"))
                .cuotaInicial(new BigDecimal("50000.00"))
                .numeroCuotas(12)
                .estado("ACTIVO")
                .build();
    }

    public static Pago pago(CronogramaPago cp, int nroCuota) {
        return Pago.builder()
                .cronograma(cp)
                .nroCuota(nroCuota)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(nroCuota))
                .estado("PENDIENTE")
                .build();
    }

    public static Pago pagoPagado(CronogramaPago cp, int nroCuota) {
        return Pago.builder()
                .cronograma(cp)
                .nroCuota(nroCuota)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().minusMonths(1))
                .estado("PAGADO")
                .montoPagado(new BigDecimal("25000.00"))
                .build();
    }

    public static Pago pagoVencido(CronogramaPago cp, int nroCuota) {
        return Pago.builder()
                .cronograma(cp)
                .nroCuota(nroCuota)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().minusDays(10))
                .estado("VENCIDO")
                .build();
    }

    public static CartaAprobacion cartaAprobacion(UsuarioActivo ua) {
        return CartaAprobacion.builder()
                .usuarioActivo(ua)
                .banco("Banco de Prueba")
                .montoAprobado(new BigDecimal("300000.00"))
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .fechaDesembolsoProyectada(LocalDate.now().plusMonths(1))
                .comentarios("Carta de aprobación de prueba")
                .build();
    }

    public static CronogramaPagoRequest crearCronogramaRequest() {
        return new CronogramaPagoRequest(
                UUID.randomUUID(),
                new BigDecimal("350000.00"),
                new BigDecimal("50000.00"),
                12,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    public static PagoRequest crearPagoRequest(Integer nroCuota) {
        return new PagoRequest(
                nroCuota,
                new BigDecimal("25000.00"),
                LocalDate.now().plusMonths(nroCuota)
        );
    }

    public static CartaAprobacionRequest crearCartaRequest() {
        return new CartaAprobacionRequest(
                UUID.randomUUID(),
                "Banco de Prueba",
                new BigDecimal("300000.00"),
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                LocalDate.now().plusMonths(1),
                "Comentarios de prueba"
        );
    }
}
