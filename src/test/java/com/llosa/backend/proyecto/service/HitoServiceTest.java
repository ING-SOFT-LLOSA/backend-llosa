package com.llosa.backend.proyecto.service;

import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.enums.TipoHito;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.service.impl.HitoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HitoServiceTest {

    @Mock HitoRepository hitoRepository;
    @Mock ProyectoService proyectoService;
    @Mock HidratationService hidratacionService;
    @Mock HitoPisoRepository hitoPisoRepository;

    @InjectMocks HitoServiceImpl service;

    private Proyecto buildProyecto() {
        return Proyecto.builder()
                .id(UUID.randomUUID())
                .nombre("Proyecto Test")
                .build();
    }

    private Hito buildHito(Proyecto proyecto) {
        return Hito.builder()
                .id(UUID.randomUUID())
                .titulo("Hito 1")
                .orden(1)
                .tipo(TipoHito.OBRA)
                .estado(EstadoHito.PENDIENTE)
                .proyecto(proyecto)
                .build();
    }

    @Test
    void save_conProyectoId_asignaProyectoYPropaga() {
        Proyecto proyecto = buildProyecto();
        Hito hito = Hito.builder()
                .titulo("Hito 1").orden(1).tipo(TipoHito.OBRA)
                .estado(EstadoHito.PENDIENTE).build();
        Hito saved = buildHito(proyecto);

        when(proyectoService.findById(proyecto.getId())).thenReturn(proyecto);
        when(hitoRepository.save(hito)).thenReturn(saved);

        Hito result = service.save(proyecto.getId(), hito);

        assertThat(result).isEqualTo(saved);
        assertThat(hito.getProyecto()).isEqualTo(proyecto);
        verify(hidratacionService).propagateMilestoneToProjectFloors(saved, proyecto.getId());
    }

    @Test
    void save_hitoDirecto_delegaEnRepository() {
        Proyecto proyecto = buildProyecto();
        Hito hito = buildHito(proyecto);
        when(hitoRepository.save(hito)).thenReturn(hito);

        Hito result = service.save(hito);

        assertThat(result).isEqualTo(hito);
        verify(hitoRepository).save(hito);
    }

    @Test
    void findById_encontrado_devuelveHito() {
        Proyecto proyecto = buildProyecto();
        Hito hito = buildHito(proyecto);
        when(hitoRepository.findById(hito.getId())).thenReturn(Optional.of(hito));

        Hito result = service.findById(hito.getId());

        assertThat(result).isEqualTo(hito);
    }

    @Test
    void findById_noEncontrado_lanzaException() {
        UUID id = UUID.randomUUID();
        when(hitoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Hito no encontrado");
    }

    @Test
    void deleteById_sinPropagaciones_eliminaOk() {
        UUID id = UUID.randomUUID();
        when(hitoPisoRepository.countByHitoId(id)).thenReturn(0L);

        service.deleteById(id);

        verify(hitoRepository).deleteById(id);
    }

    // CP14: hito propagado a unidades no puede eliminarse
    @Test
    void deleteById_conHitosPropagados_lanzaBusinessException() {
        UUID id = UUID.randomUUID();
        when(hitoPisoRepository.countByHitoId(id)).thenReturn(5L);

        assertThatThrownBy(() -> service.deleteById(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("propagado a unidades");

        verify(hitoRepository, never()).deleteById(any());
    }
}
