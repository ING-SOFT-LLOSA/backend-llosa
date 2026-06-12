package com.llosa.backend.proyecto.service;

import com.llosa.backend.annotation.CP;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.proyecto.dto.request.ReporteCreateRequest;
import com.llosa.backend.proyecto.dto.request.ReporteUpdateRequest;
import com.llosa.backend.proyecto.dto.response.ReporteResponse;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Reporte;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.ReporteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de Reportes de avance de obra (CU006 / CP21).
 * Verifica que el porcentaje de avance se recalcule a partir de los hitos
 * COMPLETADOS sobre el total y que la multimedia se recupere desde la Bóveda.
 */
@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    @Mock ReporteRepository reporteRepository;
    @Mock ProyectoRepository proyectoRepository;
    @Mock HitoRepository hitoRepository;
    @Mock DocumentoService documentoService;

    @InjectMocks ReporteServiceImpl service;

    private Proyecto proyecto(UUID id) {
        return Proyecto.builder().id(id).nombre("Edificio Aurora").build();
    }

    private Reporte reporte(UUID id, Proyecto proyecto) {
        return Reporte.builder()
                .id(id)
                .proyecto(proyecto)
                .tituloPeriodo("Mayo 2026")
                .porcentajeAvance(new BigDecimal("50.00"))
                .descripcion("Avance de acabados")
                .fecha(LocalDate.of(2026, 5, 1))
                .build();
    }

    // ─── Crear ────────────────────────────────────────────────────────────────────

    @Test
    @CP(value = "CP21", scenario = "Crear reporte recalcula avance porcentual",
            input = "3 de 4 hitos COMPLETADOS",
            expected = "porcentajeAvance = 75.00")
    @DisplayName("crear calcula el avance como completados/total * 100")
    void crear_calculaPorcentajeAvance() {
        UUID proyId = UUID.randomUUID();
        Proyecto proy = proyecto(proyId);
        when(proyectoRepository.findById(proyId)).thenReturn(Optional.of(proy));
        when(hitoRepository.countByProyectoId(proyId)).thenReturn(4L);
        when(hitoRepository.countByProyectoIdAndEstado(proyId, EstadoHito.COMPLETADO)).thenReturn(3L);
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteCreateRequest request = new ReporteCreateRequest(
                proyId, "Mayo 2026", "Avance de acabados",
                LocalDate.of(2026, 5, 1), List.of("Acabados", "Estructura"));

        ReporteResponse resp = service.crear(request);

        assertThat(resp.porcentajeAvance()).isEqualByComparingTo("75.00");
        assertThat(resp.nombreProyecto()).isEqualTo("Edificio Aurora");
        assertThat(resp.hitosConsolidados()).containsExactly("Acabados", "Estructura");
    }

    @Test
    @CP(value = "CP21", scenario = "Avance sin hitos definidos",
            input = "total de hitos = 0",
            expected = "porcentajeAvance = 0 (evita división por cero)")
    void crear_sinHitos_avanceCero() {
        UUID proyId = UUID.randomUUID();
        when(proyectoRepository.findById(proyId)).thenReturn(Optional.of(proyecto(proyId)));
        when(hitoRepository.countByProyectoId(proyId)).thenReturn(0L);
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteCreateRequest request = new ReporteCreateRequest(
                proyId, "Periodo", null, LocalDate.now(), null);

        ReporteResponse resp = service.crear(request);

        assertThat(resp.porcentajeAvance()).isEqualByComparingTo("0");
        verify(hitoRepository, never()).countByProyectoIdAndEstado(any(), any());
    }

    @Test
    void crear_proyectoInexistente_lanzaNotFound() {
        UUID proyId = UUID.randomUUID();
        when(proyectoRepository.findById(proyId)).thenReturn(Optional.empty());

        ReporteCreateRequest request = new ReporteCreateRequest(
                proyId, "Periodo", null, LocalDate.now(), null);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(reporteRepository, never()).save(any());
    }

    // ─── Obtener por id ───────────────────────────────────────────────────────────

    @Test
    @CP(value = "CP21", scenario = "Consulta de reporte con multimedia",
            input = "reporteId existente",
            expected = "Adjunta la multimedia recuperada de la Bóveda (GCS)")
    void obtenerPorId_adjuntaMultimedia() {
        UUID id = UUID.randomUUID();
        Reporte rep = reporte(id, proyecto(UUID.randomUUID()));
        when(reporteRepository.findById(id)).thenReturn(Optional.of(rep));
        DocumentoResponse media = new DocumentoResponse(
                UUID.randomUUID(), "foto.jpg", null, "image/jpeg",
                id.toString(), "REPORTE", null, "https://signed");
        when(documentoService.obtenerPorReferencia("REPORTE", id.toString()))
                .thenReturn(List.of(media));

        ReporteResponse resp = service.obtenerPorId(id);

        assertThat(resp.multimedia()).hasSize(1);
        assertThat(resp.multimedia().get(0).nombreOriginal()).isEqualTo("foto.jpg");
    }

    @Test
    void obtenerPorId_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(reporteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── Listar por proyecto ──────────────────────────────────────────────────────

    @Test
    void listarPorProyecto_mapeaCadaReporteConSuMultimedia() {
        UUID proyId = UUID.randomUUID();
        Proyecto proy = proyecto(proyId);
        Reporte rep = reporte(UUID.randomUUID(), proy);
        Pageable pageable = PageRequest.of(0, 10);

        when(proyectoRepository.existsById(proyId)).thenReturn(true);
        when(reporteRepository.findByProyectoId(proyId, pageable))
                .thenReturn(new PageImpl<>(List.of(rep)));
        when(documentoService.obtenerPorReferencia(eq("REPORTE"), anyString()))
                .thenReturn(List.of());

        Page<ReporteResponse> page = service.listarPorProyecto(proyId, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).nombreProyecto()).isEqualTo("Edificio Aurora");
    }

    @Test
    void listarPorProyecto_proyectoInexistente_lanzaNotFound() {
        UUID proyId = UUID.randomUUID();
        when(proyectoRepository.existsById(proyId)).thenReturn(false);

        assertThatThrownBy(() -> service.listarPorProyecto(proyId, PageRequest.of(0, 10)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── Actualizar ───────────────────────────────────────────────────────────────

    @Test
    void actualizar_recalculaAvanceYReemplazaHitos() {
        UUID id = UUID.randomUUID();
        UUID proyId = UUID.randomUUID();
        Reporte rep = reporte(id, proyecto(proyId));
        rep.getHitosConsolidados().add("Viejo");

        when(reporteRepository.findById(id)).thenReturn(Optional.of(rep));
        when(hitoRepository.countByProyectoId(proyId)).thenReturn(2L);
        when(hitoRepository.countByProyectoIdAndEstado(proyId, EstadoHito.COMPLETADO)).thenReturn(1L);
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteUpdateRequest request = new ReporteUpdateRequest(
                "Junio 2026", "Nueva desc", LocalDate.of(2026, 6, 1), List.of("Nuevo"));

        ReporteResponse resp = service.actualizar(id, request);

        assertThat(resp.tituloPeriodo()).isEqualTo("Junio 2026");
        assertThat(resp.porcentajeAvance()).isEqualByComparingTo("50.00");
        assertThat(resp.hitosConsolidados()).containsExactly("Nuevo");
    }

    @Test
    void actualizar_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(reporteRepository.findById(id)).thenReturn(Optional.empty());

        ReporteUpdateRequest request = new ReporteUpdateRequest(
                "T", null, LocalDate.now(), null);

        assertThatThrownBy(() -> service.actualizar(id, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── Eliminar ───────────────────────────────────────────────────────────────

    @Test
    void eliminar_existente_borra() {
        UUID id = UUID.randomUUID();
        when(reporteRepository.existsById(id)).thenReturn(true);

        service.eliminar(id);

        verify(reporteRepository).deleteById(id);
    }

    @Test
    void eliminar_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(reporteRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminar(id))
                .isInstanceOf(EntityNotFoundException.class);
        verify(reporteRepository, never()).deleteById(any());
    }
}
