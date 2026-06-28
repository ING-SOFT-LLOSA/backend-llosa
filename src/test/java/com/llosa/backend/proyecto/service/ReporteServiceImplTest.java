package com.llosa.backend.proyecto.service;

import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.proyecto.dto.request.ReporteCreateRequest;
import com.llosa.backend.proyecto.dto.request.ReporteUpdateRequest;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Reporte;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.ReporteRepository;
import com.llosa.backend.proyecto.service.impl.ReporteServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    @Mock ReporteRepository reporteRepository;
    @Mock ProyectoRepository proyectoRepository;
    @Mock HitoRepository hitoRepository;
    @Mock DocumentoService documentoService;

    @InjectMocks ReporteServiceImpl reporteService;

    private final UUID proyectoId = UUID.randomUUID();
    private final UUID reporteId = UUID.randomUUID();

    private Proyecto buildProyecto() {
        return Proyecto.builder().id(proyectoId).nombre("Test Proyecto").build();
    }

    private Reporte buildReporte() {
        return Reporte.builder()
                .id(reporteId)
                .proyecto(buildProyecto())
                .tituloPeriodo("Enero 2026")
                .porcentajeAvance(new BigDecimal("50.00"))
                .descripcion("Descripcion test")
                .fecha(LocalDate.now())
                .build();
    }
    @Test
    void crear_exitoso() {
        var request = new ReporteCreateRequest(proyectoId, "Enero 2026", "Desc", LocalDate.now(), List.of("Hito1"));
        when(proyectoRepository.findById(proyectoId)).thenReturn(Optional.of(buildProyecto()));
        when(hitoRepository.countByProyectoId(proyectoId)).thenReturn(2L);
        when(hitoRepository.countByProyectoIdAndEstado(proyectoId, EstadoHito.COMPLETADO)).thenReturn(1L);
        when(reporteRepository.save(any())).thenAnswer(inv -> {
            Reporte r = inv.getArgument(0);
            var idField = Reporte.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(r, reporteId);
            return r;
        });
        // Agregamos el mock de documentoService para que no devuelva NullPointerException
        when(documentoService.obtenerPorReferencia(eq("REPORTE"), anyString())).thenReturn(List.of());

        // Agregamos null para archivos y 1 para el usuarioId
        var result = reporteService.crear(request, null, 1);

        assertThat(result.id()).isEqualTo(reporteId);
        assertThat(result.tituloPeriodo()).isEqualTo("Enero 2026");
        assertThat(result.porcentajeAvance()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void crear_proyectoNoExiste_lanzaEntityNotFound() {
        var request = new ReporteCreateRequest(proyectoId, "Enero 2026", null, null, null);
        when(proyectoRepository.findById(proyectoId)).thenReturn(Optional.empty());

        // Agregamos null, 1
        assertThatThrownBy(() -> reporteService.crear(request, null, 1))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void crear_calcularAvance_conCeroHitos_retornaCero() {
        var request = new ReporteCreateRequest(proyectoId, "Enero 2026", null, null, null);
        when(proyectoRepository.findById(proyectoId)).thenReturn(Optional.of(buildProyecto()));
        when(hitoRepository.countByProyectoId(proyectoId)).thenReturn(0L);
        when(reporteRepository.save(any())).thenAnswer(inv -> {
            Reporte r = inv.getArgument(0);
            var idField = Reporte.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(r, reporteId);
            return r;
        });
        when(documentoService.obtenerPorReferencia(eq("REPORTE"), anyString())).thenReturn(List.of());

        // Agregamos null, 1
        var result = reporteService.crear(request, null, 1);
        assertThat(result.porcentajeAvance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void crear_hitosConsolidadosNull_usaListaVacia() {
        var request = new ReporteCreateRequest(proyectoId, "Enero 2026", null, null, null);
        when(proyectoRepository.findById(proyectoId)).thenReturn(Optional.of(buildProyecto()));
        when(hitoRepository.countByProyectoId(proyectoId)).thenReturn(1L);
        when(hitoRepository.countByProyectoIdAndEstado(proyectoId, EstadoHito.COMPLETADO)).thenReturn(0L);
        when(reporteRepository.save(any())).thenAnswer(inv -> {
            Reporte r = inv.getArgument(0);
            var idField = Reporte.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(r, reporteId); // Añadido para evitar nulos en el ID al consultar referencias
            return r;
        });
        when(documentoService.obtenerPorReferencia(eq("REPORTE"), anyString())).thenReturn(List.of());

        // Agregamos null, 1
        var result = reporteService.crear(request, null, 1);
        assertThat(result.hitosConsolidados()).isEmpty();
    }

    @Test
    void obtenerPorId_exitoso() {
        when(reporteRepository.findById(reporteId)).thenReturn(Optional.of(buildReporte()));
        when(documentoService.obtenerPorReferencia("REPORTE", reporteId.toString())).thenReturn(List.of());

        var result = reporteService.obtenerPorId(reporteId);
        assertThat(result.id()).isEqualTo(reporteId);
    }

    @Test
    void obtenerPorId_noExiste_lanzaEntityNotFound() {
        when(reporteRepository.findById(reporteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reporteService.obtenerPorId(reporteId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listarPorProyecto_exitoso() {
        var reporte = buildReporte();
        var page = new PageImpl<>(List.of(reporte));
        when(proyectoRepository.existsById(proyectoId)).thenReturn(true);
        when(reporteRepository.findByProyectoId(proyectoId, PageRequest.of(0, 10))).thenReturn(page);
        when(documentoService.obtenerPorReferencia("REPORTE", reporteId.toString())).thenReturn(List.of());

        var result = reporteService.listarPorProyecto(proyectoId, PageRequest.of(0, 10));
        assertThat(result).hasSize(1);
    }

    @Test
    void listarPorProyecto_proyectoNoExiste_lanzaEntityNotFound() {
        when(proyectoRepository.existsById(proyectoId)).thenReturn(false);

        assertThatThrownBy(() -> reporteService.listarPorProyecto(proyectoId, PageRequest.of(0, 10)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void actualizar_exitoso() {
        var reporte = buildReporte();
        var request = new ReporteUpdateRequest("Febrero 2026", "Nueva desc", LocalDate.now(), List.of("Hito2"));

        when(reporteRepository.findById(reporteId)).thenReturn(Optional.of(reporte));
        when(hitoRepository.countByProyectoId(proyectoId)).thenReturn(2L);
        when(hitoRepository.countByProyectoIdAndEstado(proyectoId, EstadoHito.COMPLETADO)).thenReturn(2L);
        when(reporteRepository.save(any())).thenReturn(reporte);

        var result = reporteService.actualizar(reporteId, request);
        assertThat(result.tituloPeriodo()).isEqualTo("Febrero 2026");
        assertThat(reporte.getPorcentajeAvance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void actualizar_noExiste_lanzaEntityNotFound() {
        var request = new ReporteUpdateRequest("Titulo", null, null, null);
        when(reporteRepository.findById(reporteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reporteService.actualizar(reporteId, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void actualizar_hitosConsolidadosNull_vaciaLista() {
        var reporte = buildReporte();
        reporte.getHitosConsolidados().add("Hito1");
        var request = new ReporteUpdateRequest("Titulo", null, null, null);

        when(reporteRepository.findById(reporteId)).thenReturn(Optional.of(reporte));
        when(hitoRepository.countByProyectoId(proyectoId)).thenReturn(1L);
        when(hitoRepository.countByProyectoIdAndEstado(proyectoId, EstadoHito.COMPLETADO)).thenReturn(0L);
        when(reporteRepository.save(any())).thenReturn(reporte);

        reporteService.actualizar(reporteId, request);
        assertThat(reporte.getHitosConsolidados()).isEmpty();
    }

    @Test
    void eliminar_exitoso() {
        when(reporteRepository.existsById(reporteId)).thenReturn(true);

        reporteService.eliminar(reporteId);

        verify(reporteRepository).deleteById(reporteId);
    }

    @Test
    void eliminar_noExiste_lanzaEntityNotFound() {
        when(reporteRepository.existsById(reporteId)).thenReturn(false);

        assertThatThrownBy(() -> reporteService.eliminar(reporteId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(reporteRepository, never()).deleteById(any());
    }
}
