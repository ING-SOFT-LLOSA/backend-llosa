package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.response.SeguimientoResponseDTO;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.service.impl.SeguimientoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeguimientoServiceTest {

    @Mock HitoPisoRepository hitoPisoRepository;

    @InjectMocks SeguimientoServiceImpl service;

    private Hito buildHito(int orden, String titulo) {
        return Hito.builder()
                .id(UUID.randomUUID())
                .titulo(titulo)
                .orden(orden)
                .estado(EstadoHito.PENDIENTE)
                .build();
    }

    private HitoPiso buildHitoPiso(Hito hito, EstadoHito estado) {
        return HitoPiso.builder()
                .id(UUID.randomUUID())
                .hito(hito)
                .piso(Piso.builder().id(1L).nroPiso(1).build())
                .estado(estado)
                .build();
    }

    @Test
    void obtenerSeguimiento_sinHitos_lanzaException() {
        UUID activoId = UUID.randomUUID();
        when(hitoPisoRepository.findByActivoIdOrderByHitoOrdenAsc(activoId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.obtenerSeguimiento(activoId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se encontraron hitos");
    }

    @Test
    void obtenerSeguimiento_conHitoEnProgreso_devuelveStepperConFaseActual() {
        UUID activoId = UUID.randomUUID();
        Hito h1 = buildHito(1, "Cimentación");
        Hito h2 = buildHito(2, "Estructura");

        HitoPiso hp1 = buildHitoPiso(h1, EstadoHito.COMPLETADO);
        HitoPiso hp2 = buildHitoPiso(h2, EstadoHito.PENDIENTE);

        when(hitoPisoRepository.findByActivoIdOrderByHitoOrdenAsc(activoId))
                .thenReturn(List.of(hp1, hp2));

        SeguimientoResponseDTO result = service.obtenerSeguimiento(activoId);

        assertThat(result.stepper()).hasSize(2);
        assertThat(result.stepper().get(0).estado()).isEqualTo("COMPLETADO");
        assertThat(result.stepper().get(1).estado()).isEqualTo("EN_PROGRESO");
        assertThat(result.faseActual().titulo()).isEqualTo("Estructura");
        assertThat(result.faseActual().porcentajeEtapa()).isEqualTo(50.0);
    }

    @Test
    void obtenerSeguimiento_todosCompletados_devuelve100Porciento() {
        UUID activoId = UUID.randomUUID();
        Hito h1 = buildHito(1, "Cimentación");
        Hito h2 = buildHito(2, "Estructura");

        HitoPiso hp1 = buildHitoPiso(h1, EstadoHito.COMPLETADO);
        HitoPiso hp2 = buildHitoPiso(h2, EstadoHito.COMPLETADO);
        hp2.setUpdatedAt(LocalDateTime.now());

        when(hitoPisoRepository.findByActivoIdOrderByHitoOrdenAsc(activoId))
                .thenReturn(List.of(hp1, hp2));

        SeguimientoResponseDTO result = service.obtenerSeguimiento(activoId);

        assertThat(result.stepper()).hasSize(2);
        assertThat(result.faseActual().titulo()).isEqualTo("Obra Finalizada");
        assertThat(result.faseActual().porcentajeEtapa()).isEqualTo(100.0);
    }

    @Test
    void obtenerSeguimiento_primerHitoPendiente_devuelveEnProgreso() {
        UUID activoId = UUID.randomUUID();
        Hito h1 = buildHito(1, "Inicio");
        Hito h2 = buildHito(2, "Estructura");

        HitoPiso hp1 = buildHitoPiso(h1, EstadoHito.PENDIENTE);
        HitoPiso hp2 = buildHitoPiso(h2, EstadoHito.PENDIENTE);

        when(hitoPisoRepository.findByActivoIdOrderByHitoOrdenAsc(activoId))
                .thenReturn(List.of(hp1, hp2));

        SeguimientoResponseDTO result = service.obtenerSeguimiento(activoId);

        assertThat(result.stepper().get(0).estado()).isEqualTo("EN_PROGRESO");
        assertThat(result.stepper().get(1).estado()).isEqualTo("PENDIENTE");
        assertThat(result.faseActual().porcentajeEtapa()).isEqualTo(0.0);
    }
}
