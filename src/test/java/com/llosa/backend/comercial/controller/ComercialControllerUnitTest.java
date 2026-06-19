package com.llosa.backend.comercial.controller;

import com.llosa.backend.comercial.dto.EtapaExpedienteEstadoRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteResponse;
import com.llosa.backend.comercial.service.EtapaExpedienteService;
import com.llosa.backend.comercial.service.HitoComercialService;
import com.llosa.backend.comercial.dto.HitoComercialRequest;
import com.llosa.backend.comercial.dto.HitoComercialResponse;
import com.llosa.backend.comercial.dto.StepperResponse;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de los controllers comerciales (sin Spring/Docker): se mockea
 * el servicio y se verifica el contrato HTTP (status + delegación). Cubren clases
 * que el CI tenía al 0% sin necesidad de levantar contexto.
 */
@ExtendWith(MockitoExtension.class)
class ComercialControllerUnitTest {

    // ── EtapaExpedienteController ─────────────────────────────────────────────

    @Mock EtapaExpedienteService etapaService;
    @InjectMocks EtapaExpedienteController etapaController;

    @Test
    void listarPorExpediente_devuelve200ConLista() {
        UUID expediente = UUID.randomUUID();
        EtapaExpedienteResponse r = mock(EtapaExpedienteResponse.class);
        when(etapaService.listarPorUsuarioActivo(expediente)).thenReturn(List.of(r));

        ResponseEntity<List<EtapaExpedienteResponse>> resp =
                etapaController.listarPorExpediente(expediente);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_devuelve200() {
        UUID uuid = UUID.randomUUID();
        EtapaExpedienteResponse r = mock(EtapaExpedienteResponse.class);
        when(etapaService.obtenerPorId(uuid)).thenReturn(r);

        ResponseEntity<EtapaExpedienteResponse> resp = etapaController.obtenerPorId(uuid);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(r);
    }

    @Test
    void crear_devuelve201() {
        UUID expediente = UUID.randomUUID();
        EtapaExpedienteRequest req = mock(EtapaExpedienteRequest.class);
        EtapaExpedienteResponse r = mock(EtapaExpedienteResponse.class);
        when(etapaService.crear(expediente, req)).thenReturn(r);

        ResponseEntity<EtapaExpedienteResponse> resp = etapaController.crear(expediente, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void actualizar_devuelve200() {
        UUID uuid = UUID.randomUUID();
        EtapaExpedienteRequest req = mock(EtapaExpedienteRequest.class);
        EtapaExpedienteResponse r = mock(EtapaExpedienteResponse.class);
        when(etapaService.actualizar(uuid, req)).thenReturn(r);

        assertThat(etapaController.actualizar(uuid, req).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actualizarEstado_devuelve200() {
        UUID uuid = UUID.randomUUID();
        EtapaExpedienteEstadoRequest req = mock(EtapaExpedienteEstadoRequest.class);
        EtapaExpedienteResponse r = mock(EtapaExpedienteResponse.class);
        when(etapaService.actualizarEstado(uuid, req)).thenReturn(r);

        assertThat(etapaController.actualizarEstado(uuid, req).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void eliminar_devuelve204YDelegaAlServicio() {
        UUID uuid = UUID.randomUUID();

        ResponseEntity<Void> resp = etapaController.eliminar(uuid);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(etapaService).eliminar(uuid);
    }

    // ── HitoComercialController ───────────────────────────────────────────────

    @Test
    void hito_crear_eliminar_actualizarEstado_stepper() {
        HitoComercialService hitoService = mock(HitoComercialService.class);
        HitoComercialController hitoController = new HitoComercialController(hitoService);

        // crearHito -> 201
        HitoComercialRequest req = mock(HitoComercialRequest.class);
        HitoComercialResponse r = mock(HitoComercialResponse.class);
        when(hitoService.crearHito(req)).thenReturn(r);
        assertThat(hitoController.crearHito(req).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // eliminarHito -> 204 + delegación
        UUID uuid = UUID.randomUUID();
        assertThat(hitoController.eliminarHito(uuid).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(hitoService).eliminarHito(uuid);

        // actualizarEstado -> 200
        EstadoHitoComercial estado = EstadoHitoComercial.values()[0];
        when(hitoService.actualizarEstado(uuid, estado)).thenReturn(r);
        assertThat(hitoController.actualizarEstado(uuid, estado).getStatusCode()).isEqualTo(HttpStatus.OK);

        // obtenerStepper -> 200
        StepperResponse stepper = mock(StepperResponse.class);
        when(hitoService.obtenerStepper(uuid)).thenReturn(stepper);
        assertThat(hitoController.obtenerStepper(uuid).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
