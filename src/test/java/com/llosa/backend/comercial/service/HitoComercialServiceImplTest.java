package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.dto.HitoComercialRequest;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.comercial.service.impl.HitoComercialServiceImpl;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HitoComercialServiceImplTest {

    @Mock HitoProcesoCompraRepository hitoRepository;
    @Mock EtapaExpedienteRepository etapaExpedienteRepository;
    @Mock UsuarioActivoRepository usuarioActivoRepository;

    @InjectMocks HitoComercialServiceImpl hitoComercialService;

    private final UUID uuidEtapa = UUID.randomUUID();
    private final UUID uuidHito = UUID.randomUUID();
    private final UUID uuidUa = UUID.randomUUID();

    private EtapaExpediente buildEtapa() {
        return EtapaExpediente.builder()
                .uuidEtapaExpediente(uuidEtapa)
                .etapaProceso(EtapaProceso.SEPARACION)
                .build();
    }

    private HitoProcesoCompra buildHito(Integer orden, EstadoHitoComercial estado) {
        return HitoProcesoCompra.builder()
                .uuidHitoComercial(orden == 1 ? uuidHito : UUID.randomUUID())
                .etapaExpediente(buildEtapa())
                .nombreHito("Hito " + orden)
                .orden(orden)
                .estado(estado)
                .build();
    }

    @Test
    void crearHito_exitoso() {
        var request = HitoComercialRequest.builder()
                .uuidEstapaExpediente(uuidEtapa)
                .nombreHito("Nuevo Hito")
                .descripcion("Desc")
                .orden(1)
                .build();

        when(etapaExpedienteRepository.findById(uuidEtapa)).thenReturn(Optional.of(buildEtapa()));
        when(hitoRepository.save(any())).thenAnswer(inv -> {
            HitoProcesoCompra h = inv.getArgument(0);
            var idField = HitoProcesoCompra.class.getDeclaredField("uuidHitoComercial");
            idField.setAccessible(true);
            idField.set(h, uuidHito);
            return h;
        });

        var result = hitoComercialService.crearHito(request);
        assertThat(result.nombreHito()).isEqualTo("Nuevo Hito");
        assertThat(result.estado()).isEqualTo(EstadoHitoComercial.PENDIENTE);
    }

    @Test
    void crearHito_etapaNoExiste_lanzaRecursoNoEncontrado() {
        var request = HitoComercialRequest.builder()
                .uuidEstapaExpediente(uuidEtapa)
                .nombreHito("Hito")
                .orden(1)
                .build();

        when(etapaExpedienteRepository.findById(uuidEtapa)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hitoComercialService.crearHito(request))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarHito_exitoso() {
        when(hitoRepository.existsById(uuidHito)).thenReturn(true);

        hitoComercialService.eliminarHito(uuidHito);

        verify(hitoRepository).deleteById(uuidHito);
    }

    @Test
    void eliminarHito_noExiste_lanzaRecursoNoEncontrado() {
        when(hitoRepository.existsById(uuidHito)).thenReturn(false);

        assertThatThrownBy(() -> hitoComercialService.eliminarHito(uuidHito))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(hitoRepository, never()).deleteById(any());
    }

    @Test
    void actualizarEstado_aCOMPLETADO_validaOrdenAnterior() {
        var hito = buildHito(2, EstadoHitoComercial.PENDIENTE);
        var hitoAnterior = buildHito(1, EstadoHitoComercial.COMPLETADO);

        when(hitoRepository.findById(uuidHito)).thenReturn(Optional.of(hito));
        when(hitoRepository.findByEtapaExpediente_UuidEtapaExpedienteAndOrden(uuidEtapa, 1))
                .thenReturn(Optional.of(hitoAnterior));
        when(hitoRepository.save(any())).thenReturn(hito);

        var result = hitoComercialService.actualizarEstado(uuidHito, EstadoHitoComercial.COMPLETADO);

        assertThat(result.estado()).isEqualTo(EstadoHitoComercial.COMPLETADO);
        assertThat(result.fechaCompletado()).isNotNull();
    }

    @Test
    void actualizarEstado_hitoAnteriorNoCompletado_lanzaIllegalState() {
        var hito = buildHito(2, EstadoHitoComercial.PENDIENTE);
        var hitoAnterior = buildHito(1, EstadoHitoComercial.PENDIENTE);

        when(hitoRepository.findById(uuidHito)).thenReturn(Optional.of(hito));
        when(hitoRepository.findByEtapaExpediente_UuidEtapaExpedienteAndOrden(uuidEtapa, 1))
                .thenReturn(Optional.of(hitoAnterior));

        assertThatThrownBy(() -> hitoComercialService.actualizarEstado(uuidHito, EstadoHitoComercial.COMPLETADO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Debe completar primero");
    }

    @Test
    void actualizarEstado_hitoAnteriorNoExiste_lanzaIllegalState() {
        var hito = buildHito(2, EstadoHitoComercial.PENDIENTE);

        when(hitoRepository.findById(uuidHito)).thenReturn(Optional.of(hito));
        when(hitoRepository.findByEtapaExpediente_UuidEtapaExpedienteAndOrden(uuidEtapa, 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> hitoComercialService.actualizarEstado(uuidHito, EstadoHitoComercial.COMPLETADO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe");
    }

    @Test
    void actualizarEstado_orden1_noValidaAnterior() {
        var hito = buildHito(1, EstadoHitoComercial.PENDIENTE);

        when(hitoRepository.findById(uuidHito)).thenReturn(Optional.of(hito));
        when(hitoRepository.save(any())).thenReturn(hito);

        var result = hitoComercialService.actualizarEstado(uuidHito, EstadoHitoComercial.COMPLETADO);

        assertThat(result.estado()).isEqualTo(EstadoHitoComercial.COMPLETADO);
        verify(hitoRepository, never()).findByEtapaExpediente_UuidEtapaExpedienteAndOrden(any(), anyInt());
    }

    @Test
    void actualizarEstado_aPENDIENTE_limpiaFechaCompletado() {
        var hito = buildHito(1, EstadoHitoComercial.COMPLETADO);

        when(hitoRepository.findById(uuidHito)).thenReturn(Optional.of(hito));
        when(hitoRepository.save(any())).thenReturn(hito);

        var result = hitoComercialService.actualizarEstado(uuidHito, EstadoHitoComercial.PENDIENTE);

        assertThat(result.estado()).isEqualTo(EstadoHitoComercial.PENDIENTE);
        assertThat(result.fechaCompletado()).isNull();
    }

    @Test
    void actualizarEstado_hitoNoExiste_lanzaRecursoNoEncontrado() {
        when(hitoRepository.findById(uuidHito)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hitoComercialService.actualizarEstado(uuidHito, EstadoHitoComercial.COMPLETADO))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerStepper_exitoso() {
        var etapa = buildEtapa();
        var hitos = List.of(buildHito(1, EstadoHitoComercial.COMPLETADO), buildHito(2, EstadoHitoComercial.PENDIENTE));

        when(usuarioActivoRepository.existsById(uuidUa)).thenReturn(true);
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByEtapaProcesoAsc(uuidUa))
                .thenReturn(List.of(etapa));
        when(hitoRepository.findByEtapaExpediente_UuidEtapaExpedienteOrderByOrdenAsc(uuidEtapa))
                .thenReturn(hitos);

        var result = hitoComercialService.obtenerStepper(uuidUa);

        assertThat(result.uuidUsuarioActivo()).isEqualTo(uuidUa);
        assertThat(result.etapas()).hasSize(1);
        assertThat(result.etapas().get(0).porcentajeAvance()).isEqualTo(50.0);
    }

    @Test
    void obtenerStepper_uaNoExiste_lanzaRecursoNoEncontrado() {
        when(usuarioActivoRepository.existsById(uuidUa)).thenReturn(false);

        assertThatThrownBy(() -> hitoComercialService.obtenerStepper(uuidUa))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerStepper_sinHitos_porcentajeCero() {
        var etapa = buildEtapa();

        when(usuarioActivoRepository.existsById(uuidUa)).thenReturn(true);
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByEtapaProcesoAsc(uuidUa))
                .thenReturn(List.of(etapa));
        when(hitoRepository.findByEtapaExpediente_UuidEtapaExpedienteOrderByOrdenAsc(uuidEtapa))
                .thenReturn(List.of());

        var result = hitoComercialService.obtenerStepper(uuidUa);
        assertThat(result.etapas().get(0).porcentajeAvance()).isZero();
    }
}
