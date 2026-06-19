package com.llosa.backend.pagos.service;

import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.config.TestDataPagos;
import com.llosa.backend.exception.EntidadDuplicadaException;
import com.llosa.backend.exception.EstadoInvalidoException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.factory.PagoFlujoFactory;
import com.llosa.backend.pagos.dto.CronogramaPagoResponse;
import com.llosa.backend.pagos.dto.ResumenResponse;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.pagos.entity.Pago;
import com.llosa.backend.pagos.repository.CronogramaPagoRepository;
import com.llosa.backend.pagos.repository.PagoRepository;
import com.llosa.backend.pagos.service.impl.CronogramaPagoServiceImpl;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CronogramaPagoServiceImplTest {

    @Mock
    CronogramaPagoRepository cronogramaPagoRepository;

    @Mock
    PagoRepository pagoRepository;

    @Mock
    UsuarioActivoRepository usuarioActivoRepository;

    @Mock
    PagoFlujoFactory pagoFlujoFactory;

    @Mock
    EtapaExpedienteRepository etapaExpedienteRepository;

    @Mock
    RequisitoDocumentalRepository requisitoDocumentalRepository;

    @InjectMocks
    CronogramaPagoServiceImpl cronogramaPagoService;

    @Test
    void crear_exitoso() {
        var request = TestDataPagos.crearCronogramaRequest();
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(request.uuidUsuarioActivo()).build();

        when(cronogramaPagoRepository.existsByUsuarioActivo_UuidUsuarioActivo(request.uuidUsuarioActivo()))
                .thenReturn(false);
        when(usuarioActivoRepository.findById(request.uuidUsuarioActivo()))
                .thenReturn(Optional.of(ua));

        ArgumentCaptor<CronogramaPago> captor = ArgumentCaptor.forClass(CronogramaPago.class);
        when(cronogramaPagoRepository.save(captor.capture())).thenAnswer(inv -> {
            CronogramaPago cp = inv.getArgument(0);
            cp.setId(UUID.randomUUID());
            return cp;
        });

        when(pagoFlujoFactory.generarPagos(any(), any())).thenReturn(List.of());

        CronogramaPagoResponse result = cronogramaPagoService.crear(request);

        assertThat(result.totalPactado()).isEqualByComparingTo(request.totalPactado());
        assertThat(result.numeroCuotas()).isEqualTo(request.numeroCuotas());
        assertThat(result.estado()).isEqualTo("ACTIVO");
    }

    @Test
    void crear_duplicado_lanzaEntidadDuplicadaException() {
        var request = TestDataPagos.crearCronogramaRequest();
        when(cronogramaPagoRepository.existsByUsuarioActivo_UuidUsuarioActivo(request.uuidUsuarioActivo()))
                .thenReturn(true);

        assertThatThrownBy(() -> cronogramaPagoService.crear(request))
                .isInstanceOf(EntidadDuplicadaException.class)
                .hasMessageContaining("ya tiene un cronograma");

        verify(usuarioActivoRepository, never()).findById(any());
    }

    @Test
    void crear_expedienteNoExiste_lanzaRecursoNoEncontrado() {
        var request = TestDataPagos.crearCronogramaRequest();
        when(cronogramaPagoRepository.existsByUsuarioActivo_UuidUsuarioActivo(request.uuidUsuarioActivo()))
                .thenReturn(false);
        when(usuarioActivoRepository.findById(request.uuidUsuarioActivo()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cronogramaPagoService.crear(request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Expediente no encontrado");
    }

    @Test
    void obtenerPorUsuarioActivo_exitoso() {
        UUID uuidUa = UUID.randomUUID();
        var cp = CronogramaPago.builder()
                .id(UUID.randomUUID())
                .usuarioActivo(UsuarioActivo.builder().uuidUsuarioActivo(uuidUa).build())
                .totalPactado(new BigDecimal("350000.00"))
                .estado("ACTIVO")
                .build();

        when(cronogramaPagoRepository.findByUsuarioActivo_UuidUsuarioActivo(uuidUa))
                .thenReturn(Optional.of(cp));

        CronogramaPagoResponse result = cronogramaPagoService.obtenerPorUsuarioActivo(uuidUa);

        assertThat(result.uuidCronograma()).isEqualTo(cp.getId());
        assertThat(result.estado()).isEqualTo("ACTIVO");
    }

    @Test
    void obtenerPorUsuarioActivo_noExiste_lanzaRecursoNoEncontrado() {
        UUID uuidUa = UUID.randomUUID();
        when(cronogramaPagoRepository.findByUsuarioActivo_UuidUsuarioActivo(uuidUa))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cronogramaPagoService.obtenerPorUsuarioActivo(uuidUa))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No hay cronograma");
    }

    @Test
    void actualizar_exitoso() {
        UUID uuidCp = UUID.randomUUID();
        var request = TestDataPagos.crearCronogramaRequest();
        var cp = CronogramaPago.builder()
                .id(uuidCp)
                .usuarioActivo(UsuarioActivo.builder().uuidUsuarioActivo(request.uuidUsuarioActivo()).build())
                .totalPactado(new BigDecimal("100000.00"))
                .estado("ACTIVO")
                .build();

        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.of(cp));
        when(cronogramaPagoRepository.save(any())).thenReturn(cp);

        CronogramaPagoResponse result = cronogramaPagoService.actualizar(uuidCp, request);

        assertThat(result.totalPactado()).isEqualByComparingTo(request.totalPactado());
        assertThat(cp.getTotalPactado()).isEqualByComparingTo(request.totalPactado());
    }

    @Test
    void actualizar_noExiste_lanzaRecursoNoEncontrado() {
        UUID uuidCp = UUID.randomUUID();
        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cronogramaPagoService.actualizar(uuidCp, TestDataPagos.crearCronogramaRequest()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Cronograma no encontrado");
    }

    @Test
    void actualizar_historico_lanzaEstadoInvalidoException() {
        UUID uuidCp = UUID.randomUUID();
        var request = TestDataPagos.crearCronogramaRequest();
        var cp = CronogramaPago.builder()
                .id(uuidCp)
                .usuarioActivo(UsuarioActivo.builder().uuidUsuarioActivo(request.uuidUsuarioActivo()).build())
                .totalPactado(new BigDecimal("100000.00"))
                .estado(CronogramaPago.ESTADO_HISTORICO)
                .build();

        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.of(cp));

        assertThatThrownBy(() -> cronogramaPagoService.actualizar(uuidCp, request))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("HISTORICO");

        verify(cronogramaPagoRepository, never()).save(any());
    }

    @Test
    void eliminar_exitoso() {
        UUID uuidCp = UUID.randomUUID();
        var cp = CronogramaPago.builder()
                .id(uuidCp)
                .estado(CronogramaPago.ESTADO_ACTIVO)
                .build();
        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.of(cp));

        cronogramaPagoService.eliminar(uuidCp);

        verify(cronogramaPagoRepository).deleteById(uuidCp);
    }

    @Test
    void eliminar_noExiste_lanzaRecursoNoEncontrado() {
        UUID uuidCp = UUID.randomUUID();
        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cronogramaPagoService.eliminar(uuidCp))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Cronograma no encontrado");

        verify(cronogramaPagoRepository, never()).deleteById(any());
    }

    @Test
    void eliminar_historico_lanzaEstadoInvalidoException() {
        UUID uuidCp = UUID.randomUUID();
        var cp = CronogramaPago.builder()
                .id(uuidCp)
                .estado(CronogramaPago.ESTADO_HISTORICO)
                .build();
        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.of(cp));

        assertThatThrownBy(() -> cronogramaPagoService.eliminar(uuidCp))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("HISTORICO");

        verify(cronogramaPagoRepository, never()).deleteById(any());
    }

    @Test
    void obtenerResumen_conPagos_devuelveResumenCorrecto() {
        UUID uuidCp = UUID.randomUUID();
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(UUID.randomUUID()).build();
        var cp = CronogramaPago.builder()
                .id(uuidCp)
                .usuarioActivo(ua)
                .totalPactado(new BigDecimal("100000.00"))
                .estado("ACTIVO")
                .build();

        var pago1 = Pago.builder()
                .nroCuota(1)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().plusMonths(1))
                .estado("PENDIENTE")
                .build();
        var pago2 = Pago.builder()
                .nroCuota(2)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now().minusDays(5))
                .estado("PAGADO")
                .montoPagado(new BigDecimal("25000.00"))
                .build();

        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.of(cp));
        when(pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(uuidCp))
                .thenReturn(List.of(pago1, pago2));

        ResumenResponse resumen = cronogramaPagoService.obtenerResumen(uuidCp);

        assertThat(resumen.totalPactado()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(resumen.totalPagado()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(resumen.totalPendiente()).isEqualByComparingTo(new BigDecimal("75000.00"));
        assertThat(resumen.cuotasPagadas()).isEqualTo(1);
        assertThat(resumen.cuotasPendientes()).isEqualTo(1);
        assertThat(resumen.cuotasVencidas()).isEqualTo(0);
        assertThat(resumen.estadoGlobal()).isEqualTo("AL_DIA");
    }

    @Test
    void obtenerResumen_conCuotaVencida_devuelveEN_MORA() {
        UUID uuidCp = UUID.randomUUID();
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(UUID.randomUUID()).build();
        var cp = CronogramaPago.builder()
                .id(uuidCp)
                .usuarioActivo(ua)
                .totalPactado(new BigDecimal("50000.00"))
                .estado("ACTIVO")
                .build();

        var pagoVencido = TestDataPagos.pagoVencido(cp, 1);

        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.of(cp));
        when(pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(uuidCp))
                .thenReturn(List.of(pagoVencido));

        ResumenResponse resumen = cronogramaPagoService.obtenerResumen(uuidCp);

        assertThat(resumen.estadoGlobal()).isEqualTo("EN_MORA");
        assertThat(resumen.cuotasVencidas()).isEqualTo(1);
    }

    @Test
    void obtenerResumen_todasPagadas_devuelveLIQUIDADO() {
        UUID uuidCp = UUID.randomUUID();
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(UUID.randomUUID()).build();
        var cp = CronogramaPago.builder()
                .id(uuidCp)
                .usuarioActivo(ua)
                .totalPactado(new BigDecimal("50000.00"))
                .estado("ACTIVO")
                .build();

        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.of(cp));
        when(pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(uuidCp))
                .thenReturn(List.of(TestDataPagos.pagoPagado(cp, 1), TestDataPagos.pagoPagado(cp, 2)));

        ResumenResponse resumen = cronogramaPagoService.obtenerResumen(uuidCp);

        assertThat(resumen.estadoGlobal()).isEqualTo("LIQUIDADO");
        assertThat(resumen.cuotasPagadas()).isEqualTo(2);
    }

    @Test
    void obtenerResumen_sinPagos_estadoACTIVO() {
        UUID uuidCp = UUID.randomUUID();
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(UUID.randomUUID()).build();
        var cp = CronogramaPago.builder()
                .id(uuidCp)
                .usuarioActivo(ua)
                .totalPactado(new BigDecimal("50000.00"))
                .estado("ACTIVO")
                .build();

        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.of(cp));
        when(pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(uuidCp))
                .thenReturn(List.of());

        ResumenResponse resumen = cronogramaPagoService.obtenerResumen(uuidCp);

        assertThat(resumen.estadoGlobal()).isEqualTo("ACTIVO");
        assertThat(resumen.totalPagado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void obtenerResumen_cronogramaNoExiste_lanzaRecursoNoEncontrado() {
        UUID uuidCp = UUID.randomUUID();
        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cronogramaPagoService.obtenerResumen(uuidCp))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Cronograma no encontrado");
    }
}
