package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.ReporteCreateRequest;
import com.llosa.backend.proyecto.dto.request.ReporteUpdateRequest;
import com.llosa.backend.proyecto.dto.response.ReporteResponse;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del controlador REST de Reportes (CU006 / CP21). Verifica
 * el contrato HTTP: códigos de estado y delegación correcta al servicio.
 */
@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock ReporteService reporteService;
    @Mock ActivoService activoService;

    @InjectMocks ReporteController controller;

    private ReporteResponse dummy(UUID id) {
        return new ReporteResponse(id, UUID.randomUUID(), "Aurora", "Mayo",
                null, "desc", List.of(), null, List.of());
    }

    @Test
    void crear_devuelve201() {
        ReporteCreateRequest req = new ReporteCreateRequest(
                UUID.randomUUID(), "Mayo", null, null, null);
        ReporteResponse resp = dummy(UUID.randomUUID());
        when(reporteService.crear(req)).thenReturn(resp);

        ResponseEntity<ReporteResponse> r = controller.crear(req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getBody()).isEqualTo(resp);
    }

    @Test
    void obtenerPorId_devuelve200() {
        UUID id = UUID.randomUUID();
        when(reporteService.obtenerPorId(id)).thenReturn(dummy(id));

        ResponseEntity<ReporteResponse> r = controller.obtenerPorId(id);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listarPorProyecto_devuelvePagina() {
        UUID proyId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReporteResponse> page = new PageImpl<>(List.of(dummy(UUID.randomUUID())));
        when(reporteService.listarPorProyecto(proyId, pageable)).thenReturn(page);

        ResponseEntity<Page<ReporteResponse>> r = controller.listarPorProyecto(proyId, pageable);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().getTotalElements()).isEqualTo(1);
    }

    @Test
    void listarPorActivoProyecto_resuelveProyectoDesdeElActivo() {
        UUID activoId = UUID.randomUUID();
        UUID proyId = UUID.randomUUID();
        Proyecto proyecto = Proyecto.builder().id(proyId).nombre("Aurora").build();
        Torre torre = Torre.builder().id(1L).proyecto(proyecto).build();
        Piso piso = Piso.builder().id(1L).nroPiso(5).torre(torre).build();
        Activo activo = Activo.builder().id(activoId).nro("501").piso(piso).build();
        Pageable pageable = PageRequest.of(0, 5);

        when(activoService.findById(activoId)).thenReturn(activo);
        when(reporteService.listarPorProyecto(proyId, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<Page<ReporteResponse>> r =
                controller.listarPorActivoProyecto(activoId, pageable);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(reporteService).listarPorProyecto(proyId, pageable);
    }

    @Test
    void actualizar_devuelve200() {
        UUID id = UUID.randomUUID();
        ReporteUpdateRequest req = new ReporteUpdateRequest("Junio", null, null, null);
        when(reporteService.actualizar(id, req)).thenReturn(dummy(id));

        ResponseEntity<ReporteResponse> r = controller.actualizar(id, req);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void eliminar_devuelve204() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> r = controller.eliminar(id);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(reporteService).eliminar(id);
    }
}
