package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.dto.EtapaExpedienteEstadoRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteResponse;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.enums.EstadoEtapaExpediente;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.comercial.service.impl.EtapaExpedienteServiceImpl;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import jakarta.persistence.EntityNotFoundException;
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
class EtapaExpedienteServiceImplTest {

    @Mock EtapaExpedienteRepository etapaExpedienteRepository;
    @Mock UsuarioActivoRepository usuarioActivoRepository;

    @InjectMocks EtapaExpedienteServiceImpl etapaExpedienteService;

    private final UUID uuidEtapa = UUID.randomUUID();
    private final UUID uuidUa = UUID.randomUUID();

    private EtapaExpediente buildEtapa() {
        return EtapaExpediente.builder()
                .uuidEtapaExpediente(uuidEtapa)
                .usuarioActivo(UsuarioActivo.builder().uuidUsuarioActivo(uuidUa).build())
                .etapaProceso(EtapaProceso.SEPARACION)
                .estado(EstadoEtapaExpediente.PENDIENTE)
                .hitosComerciales(new ArrayList<>())
                .requisitos(new ArrayList<>())
                .build();
    }

    @Test
    void listarPorUsuarioActivo_exitoso() {
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByEtapaProcesoAsc(uuidUa))
                .thenReturn(List.of(buildEtapa()));

        var result = etapaExpedienteService.listarPorUsuarioActivo(uuidUa);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).etapaProceso()).isEqualTo(EtapaProceso.SEPARACION);
    }

    @Test
    void listarPorUsuarioActivo_vacio_retornaListaVacia() {
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByEtapaProcesoAsc(uuidUa))
                .thenReturn(List.of());

        assertThat(etapaExpedienteService.listarPorUsuarioActivo(uuidUa)).isEmpty();
    }

    @Test
    void obtenerPorId_exitoso() {
        when(etapaExpedienteRepository.findById(uuidEtapa)).thenReturn(Optional.of(buildEtapa()));

        var result = etapaExpedienteService.obtenerPorId(uuidEtapa);
        assertThat(result.uuidEtapaExpediente()).isEqualTo(uuidEtapa);
    }

    @Test
    void obtenerPorId_noExiste_lanzaEntityNotFound() {
        when(etapaExpedienteRepository.findById(uuidEtapa)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etapaExpedienteService.obtenerPorId(uuidEtapa))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void crear_exitoso() {
        var request = new EtapaExpedienteRequest(EtapaProceso.CONTRATO, null);
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuidUa).build();

        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.existsByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.CONTRATO))
                .thenReturn(false);
        when(etapaExpedienteRepository.save(any())).thenAnswer(inv -> {
            EtapaExpediente e = inv.getArgument(0);
            var idField = EtapaExpediente.class.getDeclaredField("uuidEtapaExpediente");
            idField.setAccessible(true);
            idField.set(e, uuidEtapa);
            return e;
        });

        var result = etapaExpedienteService.crear(uuidUa, request);
        assertThat(result.etapaProceso()).isEqualTo(EtapaProceso.CONTRATO);
        assertThat(result.estado()).isEqualTo(EstadoEtapaExpediente.PENDIENTE);
    }

    @Test
    void crear_uaNoExiste_lanzaEntityNotFound() {
        var request = new EtapaExpedienteRequest(EtapaProceso.CONTRATO, null);
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etapaExpedienteService.crear(uuidUa, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void crear_duplicado_lanzaIllegalState() {
        var request = new EtapaExpedienteRequest(EtapaProceso.CONTRATO, null);
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuidUa).build();

        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.existsByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.CONTRATO))
                .thenReturn(true);

        assertThatThrownBy(() -> etapaExpedienteService.crear(uuidUa, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void crear_conEstadoExplicito_usaEseEstado() {
        var request = new EtapaExpedienteRequest(EtapaProceso.CONTRATO, EstadoEtapaExpediente.COMPLETADO);
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuidUa).build();

        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.existsByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.CONTRATO))
                .thenReturn(false);
        when(etapaExpedienteRepository.save(any())).thenAnswer(inv -> {
            EtapaExpediente e = inv.getArgument(0);
            var idField = EtapaExpediente.class.getDeclaredField("uuidEtapaExpediente");
            idField.setAccessible(true);
            idField.set(e, uuidEtapa);
            return e;
        });

        var result = etapaExpedienteService.crear(uuidUa, request);
        assertThat(result.estado()).isEqualTo(EstadoEtapaExpediente.COMPLETADO);
    }

    @Test
    void actualizar_exitoso() {
        var etapa = buildEtapa();
        var request = new EtapaExpedienteRequest(EtapaProceso.PAGO, EstadoEtapaExpediente.COMPLETADO);

        when(etapaExpedienteRepository.findById(uuidEtapa)).thenReturn(Optional.of(etapa));
        when(etapaExpedienteRepository.save(any())).thenReturn(etapa);

        var result = etapaExpedienteService.actualizar(uuidEtapa, request);
        assertThat(result.etapaProceso()).isEqualTo(EtapaProceso.PAGO);
        assertThat(result.estado()).isEqualTo(EstadoEtapaExpediente.COMPLETADO);
    }

    @Test
    void actualizar_noExiste_lanzaEntityNotFound() {
        var request = new EtapaExpedienteRequest(EtapaProceso.PAGO, null);
        when(etapaExpedienteRepository.findById(uuidEtapa)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etapaExpedienteService.actualizar(uuidEtapa, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void actualizar_estadoNull_noCambiaEstado() {
        var etapa = buildEtapa();
        var request = new EtapaExpedienteRequest(EtapaProceso.PAGO, null);

        when(etapaExpedienteRepository.findById(uuidEtapa)).thenReturn(Optional.of(etapa));
        when(etapaExpedienteRepository.save(any())).thenReturn(etapa);

        var result = etapaExpedienteService.actualizar(uuidEtapa, request);
        assertThat(result.estado()).isEqualTo(EstadoEtapaExpediente.PENDIENTE);
    }

    @Test
    void actualizarEstado_exitoso() {
        var etapa = buildEtapa();
        var request = new EtapaExpedienteEstadoRequest(EstadoEtapaExpediente.COMPLETADO);

        when(etapaExpedienteRepository.findById(uuidEtapa)).thenReturn(Optional.of(etapa));
        when(etapaExpedienteRepository.save(any())).thenReturn(etapa);

        var result = etapaExpedienteService.actualizarEstado(uuidEtapa, request);
        assertThat(result.estado()).isEqualTo(EstadoEtapaExpediente.COMPLETADO);
    }

    @Test
    void actualizarEstado_noExiste_lanzaEntityNotFound() {
        var request = new EtapaExpedienteEstadoRequest(EstadoEtapaExpediente.COMPLETADO);
        when(etapaExpedienteRepository.findById(uuidEtapa)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etapaExpedienteService.actualizarEstado(uuidEtapa, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void eliminar_exitoso() {
        when(etapaExpedienteRepository.existsById(uuidEtapa)).thenReturn(true);

        etapaExpedienteService.eliminar(uuidEtapa);

        verify(etapaExpedienteRepository).deleteById(uuidEtapa);
    }

    @Test
    void eliminar_noExiste_lanzaEntityNotFound() {
        when(etapaExpedienteRepository.existsById(uuidEtapa)).thenReturn(false);

        assertThatThrownBy(() -> etapaExpedienteService.eliminar(uuidEtapa))
                .isInstanceOf(EntityNotFoundException.class);

        verify(etapaExpedienteRepository, never()).deleteById(any());
    }

    @Test
    void listar_mapeaTotalHitosYRequisitos() {
        var etapa = buildEtapa();
        etapa.getHitosComerciales().add(com.llosa.backend.comercial.entity.HitoProcesoCompra.builder().build());
        etapa.getRequisitos().add(com.llosa.backend.comercial.entity.RequisitoDocumental.builder().build());

        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByEtapaProcesoAsc(uuidUa))
                .thenReturn(List.of(etapa));

        var result = etapaExpedienteService.listarPorUsuarioActivo(uuidUa);
        assertThat(result.get(0).totalHitos()).isEqualTo(1);
        assertThat(result.get(0).totalRequisitos()).isEqualTo(1);
    }
}
