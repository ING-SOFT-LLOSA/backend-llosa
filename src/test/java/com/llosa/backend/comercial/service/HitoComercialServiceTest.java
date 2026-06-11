package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.dto.HitoComercialRequest;
import com.llosa.backend.comercial.dto.HitoComercialResponse;
import com.llosa.backend.comercial.dto.StepperResponse;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.comercial.service.impl.HitoComercialServiceImpl;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HitoComercialServiceTest {

    @Mock HitoProcesoCompraRepository hitoRepository;
    @Mock UsuarioActivoRepository usuarioActivoRepository;
    @Mock com.llosa.backend.pagos.repository.CronogramaPagoRepository cronogramaPagoRepository;
    @Mock com.llosa.backend.pagos.repository.PagoRepository pagoRepository;

    @InjectMocks HitoComercialServiceImpl service;

    private UsuarioActivo buildUsuarioActivo() {
        UUID uaId = UUID.randomUUID();
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        Activo activo = Activo.builder()
                .id(UUID.randomUUID()).nro("101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.ZERO).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.ZERO).descripcion("").piso(piso)
                .build();
        return UsuarioActivo.builder()
                .uuidUsuarioActivo(uaId)
                .activo(activo)
                .build();
    }

    private HitoProcesoCompra buildHito(UsuarioActivo ua, int orden, EstadoHitoComercial estado) {
        return HitoProcesoCompra.builder()
                .uuidHitoComercial(UUID.randomUUID())
                .usuarioActivo(ua)
                .etapaProceso(EtapaProceso.SEPARACION)
                .nombreHito("Hito " + orden)
                .descripcion("Desc")
                .orden(orden)
                .estado(estado)
                .build();
    }

    @Test
    void crearHito_usuarioActivoNoEncontrado_lanzaException() {
        UUID uaId = UUID.randomUUID();
        when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.empty());

        HitoComercialRequest req = new HitoComercialRequest(
                uaId, EtapaProceso.SEPARACION, "Hito 1", "Desc", 1);

        assertThatThrownBy(() -> service.crearHito(req))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearHito_exitoso_devuelveResponse() {
        UsuarioActivo ua = buildUsuarioActivo();
        HitoProcesoCompra hito = buildHito(ua, 1, EstadoHitoComercial.PENDIENTE);

        when(usuarioActivoRepository.findById(ua.getUuidUsuarioActivo())).thenReturn(Optional.of(ua));
        when(hitoRepository.save(any())).thenReturn(hito);

        HitoComercialRequest req = new HitoComercialRequest(
                ua.getUuidUsuarioActivo(), EtapaProceso.SEPARACION, "Hito 1", "Desc", 1);

        HitoComercialResponse result = service.crearHito(req);

        assertThat(result).isNotNull();
        assertThat(result.nombreHito()).isEqualTo("Hito 1");
    }

    @Test
    void eliminarHito_noEncontrado_lanzaException() {
        UUID id = UUID.randomUUID();
        when(hitoRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminarHito(id))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarHito_existente_eliminaOk() {
        UUID id = UUID.randomUUID();
        when(hitoRepository.existsById(id)).thenReturn(true);

        service.eliminarHito(id);

        verify(hitoRepository).deleteById(id);
    }

    @Test
    void actualizarEstado_noEncontrado_lanzaException() {
        UUID id = UUID.randomUUID();
        when(hitoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarEstado(id, EstadoHitoComercial.COMPLETADO))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarEstado_completado_primerHito_guardaConFecha() {
        UsuarioActivo ua = buildUsuarioActivo();
        HitoProcesoCompra hito = buildHito(ua, 1, EstadoHitoComercial.PENDIENTE);

        when(hitoRepository.findById(hito.getUuidHitoComercial())).thenReturn(Optional.of(hito));
        when(hitoRepository.save(hito)).thenReturn(hito);

        HitoComercialResponse result = service.actualizarEstado(
                hito.getUuidHitoComercial(), EstadoHitoComercial.COMPLETADO);

        assertThat(result.estado()).isEqualTo(EstadoHitoComercial.COMPLETADO);
        assertThat(hito.getFechaCompletado()).isNotNull();
    }

    @Test
    void actualizarEstado_completado_hitoAnteriorNoCompletado_lanzaIllegalState() {
        UsuarioActivo ua = buildUsuarioActivo();
        HitoProcesoCompra hitoAnterior = buildHito(ua, 1, EstadoHitoComercial.PENDIENTE);
        HitoProcesoCompra hitoActual = buildHito(ua, 2, EstadoHitoComercial.PENDIENTE);

        when(hitoRepository.findById(hitoActual.getUuidHitoComercial()))
                .thenReturn(Optional.of(hitoActual));
        when(hitoRepository.findByUsuarioActivo_UuidUsuarioActivoAndOrden(
                ua.getUuidUsuarioActivo(), 1))
                .thenReturn(Optional.of(hitoAnterior));

        assertThatThrownBy(() -> service.actualizarEstado(
                hitoActual.getUuidHitoComercial(), EstadoHitoComercial.COMPLETADO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hito anterior");
    }

    @Test
    void actualizarEstado_pendiente_limpiafechaCompletado() {
        UsuarioActivo ua = buildUsuarioActivo();
        HitoProcesoCompra hito = buildHito(ua, 1, EstadoHitoComercial.COMPLETADO);

        when(hitoRepository.findById(hito.getUuidHitoComercial())).thenReturn(Optional.of(hito));
        when(hitoRepository.save(hito)).thenReturn(hito);

        service.actualizarEstado(hito.getUuidHitoComercial(), EstadoHitoComercial.PENDIENTE);

        assertThat(hito.getFechaCompletado()).isNull();
    }

    @Test
    void obtenerStepper_usuarioActivoNoEncontrado_lanzaException() {
        UUID uaId = UUID.randomUUID();
        when(usuarioActivoRepository.existsById(uaId)).thenReturn(false);

        assertThatThrownBy(() -> service.obtenerStepper(uaId))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerStepper_existente_devuelveEtapas() {
        UsuarioActivo ua = buildUsuarioActivo();
        HitoProcesoCompra hito = buildHito(ua, 1, EstadoHitoComercial.COMPLETADO);

        when(usuarioActivoRepository.existsById(ua.getUuidUsuarioActivo())).thenReturn(true);
        when(hitoRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(
                ua.getUuidUsuarioActivo())).thenReturn(List.of(hito));

        StepperResponse result = service.obtenerStepper(ua.getUuidUsuarioActivo());

        assertThat(result).isNotNull();
        assertThat(result.etapas()).isNotEmpty();
        assertThat(result.uuidUsuarioActivo()).isEqualTo(ua.getUuidUsuarioActivo());
    }

    @Test
    void obtenerStepper_sinHitos_devuelveEtapasConCeroPorcentaje() {
        UUID uaId = UUID.randomUUID();
        when(usuarioActivoRepository.existsById(uaId)).thenReturn(true);
        when(hitoRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(uaId))
                .thenReturn(List.of());

        StepperResponse result = service.obtenerStepper(uaId);

        assertThat(result.etapas()).allMatch(e -> e.porcentajeAvance() == 0.0);
    }
}
