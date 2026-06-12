package com.llosa.backend.proyecto.service;

import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponsePorcentajeDTO;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.service.impl.HitoPisoServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HitoPisoServiceTest {

    @Mock HitoPisoRepository hitoPisoRepository;

    @InjectMocks HitoPisoServiceImpl service;

    private Hito buildHito(int orden) {
        return Hito.builder()
                .id(UUID.randomUUID())
                .titulo("Hito " + orden)
                .orden(orden)
                .estado(EstadoHito.PENDIENTE)
                .build();
    }

    private HitoPiso buildHitoPiso(Hito hito, Piso piso, EstadoHito estado) {
        return HitoPiso.builder()
                .id(UUID.randomUUID())
                .hito(hito)
                .piso(piso)
                .estado(estado)
                .build();
    }

    @Test
    void findById_encontrado_devuelveHitoPiso() {
        Hito hito = buildHito(1);
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        HitoPiso hp = buildHitoPiso(hito, piso, EstadoHito.PENDIENTE);
        when(hitoPisoRepository.findById(hp.getId())).thenReturn(Optional.of(hp));

        HitoPiso result = service.findById(hp.getId());

        assertThat(result).isEqualTo(hp);
    }

    @Test
    void findById_noEncontrado_lanzaEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(hitoPisoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Hito de unidad no encontrado");
    }

    @Test
    void cambiarEstado_completado_primerHito_guardaConFecha() {
        Hito hito = buildHito(1);
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        HitoPiso hp = buildHitoPiso(hito, piso, EstadoHito.PENDIENTE);

        when(hitoPisoRepository.findById(hp.getId())).thenReturn(Optional.of(hp));
        when(hitoPisoRepository.save(hp)).thenReturn(hp);

        HitoPiso result = service.cambiarEstado(hp.getId(), EstadoHito.COMPLETADO);

        assertThat(result.getEstado()).isEqualTo(EstadoHito.COMPLETADO);
        assertThat(result.getFechaCompletado()).isNotNull();
    }

    @Test
    void cambiarEstado_pendiente_limpiafechaCompletado() {
        Hito hito = buildHito(1);
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        HitoPiso hp = buildHitoPiso(hito, piso, EstadoHito.COMPLETADO);

        when(hitoPisoRepository.findById(hp.getId())).thenReturn(Optional.of(hp));
        when(hitoPisoRepository.save(hp)).thenReturn(hp);

        HitoPiso result = service.cambiarEstado(hp.getId(), EstadoHito.PENDIENTE);

        assertThat(result.getFechaCompletado()).isNull();
    }

    @Test
    void cambiarEstado_completado_hitoAnteriorNoCompletado_lanzaBusinessException() {
        Hito hitoAnterior = buildHito(1);
        Hito hitoActual = buildHito(2);
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();

        HitoPiso hpAnterior = buildHitoPiso(hitoAnterior, piso, EstadoHito.PENDIENTE);
        HitoPiso hpActual = buildHitoPiso(hitoActual, piso, EstadoHito.PENDIENTE);

        when(hitoPisoRepository.findById(hpActual.getId())).thenReturn(Optional.of(hpActual));
        when(hitoPisoRepository.findByPisoAndHito_Orden(piso, 1))
                .thenReturn(Optional.of(hpAnterior));

        assertThatThrownBy(() -> service.cambiarEstado(hpActual.getId(), EstadoHito.COMPLETADO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hito anterior");
    }

    @Test
    void cambiarEstado_completado_hitoAnteriorCompletado_guardaOk() {
        Hito hitoAnterior = buildHito(1);
        Hito hitoActual = buildHito(2);
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();

        HitoPiso hpAnterior = buildHitoPiso(hitoAnterior, piso, EstadoHito.COMPLETADO);
        HitoPiso hpActual = buildHitoPiso(hitoActual, piso, EstadoHito.PENDIENTE);

        when(hitoPisoRepository.findById(hpActual.getId())).thenReturn(Optional.of(hpActual));
        when(hitoPisoRepository.findByPisoAndHito_Orden(piso, 1))
                .thenReturn(Optional.of(hpAnterior));
        when(hitoPisoRepository.save(hpActual)).thenReturn(hpActual);

        HitoPiso result = service.cambiarEstado(hpActual.getId(), EstadoHito.COMPLETADO);

        assertThat(result.getEstado()).isEqualTo(EstadoHito.COMPLETADO);
    }

    @Test
    void findByActivo_devuelveListaOrdenada() {
        UUID activoId = UUID.randomUUID();
        Hito hito = buildHito(1);
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        HitoPiso hp = buildHitoPiso(hito, piso, EstadoHito.PENDIENTE);
        when(hitoPisoRepository.findByActivoIdOrderByHitoOrdenAsc(activoId)).thenReturn(List.of(hp));

        List<HitoPiso> result = service.findByActivo(activoId);

        assertThat(result).hasSize(1);
    }

    @Test
    void obtenerAvancesPorActivo_sinHitos_devuelveListaVacia() {
        UUID activoId = UUID.randomUUID();
        when(hitoPisoRepository.findByActivoIdOrderByHitoOrdenAsc(activoId)).thenReturn(List.of());

        List<AvanceUnidadResponsePorcentajeDTO> result = service.obtenerAvancesPorActivo(activoId);

        assertThat(result).isEmpty();
    }

    @Test
    void obtenerAvancesPorActivo_conHitos_calculaPorcentaje() {
        UUID activoId = UUID.randomUUID();
        Hito h1 = buildHito(1);
        Hito h2 = buildHito(2);
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        HitoPiso hp1 = buildHitoPiso(h1, piso, EstadoHito.COMPLETADO);
        HitoPiso hp2 = buildHitoPiso(h2, piso, EstadoHito.PENDIENTE);

        when(hitoPisoRepository.findByActivoIdOrderByHitoOrdenAsc(activoId))
                .thenReturn(List.of(hp1, hp2));

        List<AvanceUnidadResponsePorcentajeDTO> result = service.obtenerAvancesPorActivo(activoId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).porcentaje()).isEqualTo(50);
    }

}
