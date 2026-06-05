package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.*;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.PisoRepository;
import com.llosa.backend.proyecto.service.impl.HidratationServiceImpl;
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
class HidratationServiceTest {

    @Mock HitoRepository hitoRepository;
    @Mock HitoPisoRepository hitoPisoRepository;
    @Mock PisoRepository pisoRepository;

    @InjectMocks HidratationServiceImpl service;

    private Proyecto buildProyecto() {
        return Proyecto.builder().id(UUID.randomUUID()).nombre("Test").build();
    }

    private Torre buildTorre(Proyecto proyecto) {
        return Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();
    }

    private Piso buildPiso(Torre torre) {
        return Piso.builder().id(1L).nroPiso(1).torre(torre).build();
    }

    private Hito buildHito(UUID proyectoId) {
        Proyecto p = Proyecto.builder().id(proyectoId).build();
        return Hito.builder()
                .id(UUID.randomUUID())
                .titulo("Hito Test")
                .orden(1)
                .estado(EstadoHito.PENDIENTE)
                .proyecto(p)
                .build();
    }

    @Test
    void hydrateFloorMilestones_pisoNoEncontrado_lanzaException() {
        when(pisoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.hydrateFloorMilestones(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Piso no encontrado");
    }

    @Test
    void hydrateFloorMilestones_sinHitos_noGuardaNada() {
        Proyecto proyecto = buildProyecto();
        Torre torre = buildTorre(proyecto);
        Piso piso = buildPiso(torre);

        when(pisoRepository.findById(1L)).thenReturn(Optional.of(piso));
        when(hitoRepository.findByProyectoId(proyecto.getId())).thenReturn(List.of());

        service.hydrateFloorMilestones(1L);

        verify(hitoPisoRepository, never()).saveAll(any());
    }

    @Test
    void hydrateFloorMilestones_conHitosNuevos_guardaJunturas() {
        Proyecto proyecto = buildProyecto();
        Torre torre = buildTorre(proyecto);
        Piso piso = buildPiso(torre);
        Hito hito = buildHito(proyecto.getId());

        when(pisoRepository.findById(1L)).thenReturn(Optional.of(piso));
        when(hitoRepository.findByProyectoId(proyecto.getId())).thenReturn(List.of(hito));
        when(hitoPisoRepository.existsByPisoIdAndHitoId(1L, hito.getId())).thenReturn(false);

        service.hydrateFloorMilestones(1L);

        verify(hitoPisoRepository).saveAll(argThat(list ->
                ((List<?>) list).size() == 1
        ));
    }

    @Test
    void hydrateFloorMilestones_hitosYaExistentes_noCreaDuplicados() {
        Proyecto proyecto = buildProyecto();
        Torre torre = buildTorre(proyecto);
        Piso piso = buildPiso(torre);
        Hito hito = buildHito(proyecto.getId());

        when(pisoRepository.findById(1L)).thenReturn(Optional.of(piso));
        when(hitoRepository.findByProyectoId(proyecto.getId())).thenReturn(List.of(hito));
        when(hitoPisoRepository.existsByPisoIdAndHitoId(1L, hito.getId())).thenReturn(true);

        service.hydrateFloorMilestones(1L);

        verify(hitoPisoRepository, never()).saveAll(any());
    }

    @Test
    void propagateMilestoneToProjectFloors_creaNuevasJunturas() {
        UUID idProyecto = UUID.randomUUID();
        Proyecto proyecto = Proyecto.builder().id(idProyecto).build();
        Torre torre = buildTorre(proyecto);
        Piso piso = buildPiso(torre);
        Hito hito = buildHito(idProyecto);

        when(pisoRepository.findByTorreProyectoId(idProyecto)).thenReturn(List.of(piso));
        when(hitoPisoRepository.existsByPisoIdAndHitoId(1L, hito.getId())).thenReturn(false);

        service.propagateMilestoneToProjectFloors(hito, idProyecto);

        verify(hitoPisoRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
    }

    @Test
    void propagateMilestoneToProjectFloors_junturaExistente_noCreaDuplicado() {
        UUID idProyecto = UUID.randomUUID();
        Proyecto proyecto = Proyecto.builder().id(idProyecto).build();
        Torre torre = buildTorre(proyecto);
        Piso piso = buildPiso(torre);
        Hito hito = buildHito(idProyecto);

        when(pisoRepository.findByTorreProyectoId(idProyecto)).thenReturn(List.of(piso));
        when(hitoPisoRepository.existsByPisoIdAndHitoId(1L, hito.getId())).thenReturn(true);

        service.propagateMilestoneToProjectFloors(hito, idProyecto);

        verify(hitoPisoRepository, never()).saveAll(any());
    }

    @Test
    void hidratarActivos_llamaHydratePorCadaPiso() {
        Proyecto proyecto = buildProyecto();
        Torre torre = buildTorre(proyecto);
        Piso piso = buildPiso(torre);
        Activo activo = Activo.builder()
                .id(UUID.randomUUID()).nro("101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.ZERO).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.ZERO).descripcion("").piso(piso)
                .build();

        when(pisoRepository.findById(1L)).thenReturn(Optional.of(piso));
        when(hitoRepository.findByProyectoId(proyecto.getId())).thenReturn(List.of());

        service.hidratarActivos(List.of(activo), proyecto.getId());

        verify(pisoRepository).findById(1L);
    }
}
