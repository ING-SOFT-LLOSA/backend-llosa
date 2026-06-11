package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.service.impl.ActivoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivoServiceTest {

    @Mock ActivoRepository activoRepository;
    @Mock PisoService pisoService;
    @Mock HidratationService hidratationService;

    @InjectMocks ActivoServiceImpl service;

    private Piso buildPiso() {
        Proyecto proyecto = Proyecto.builder().nombre("Proyecto Test").build();
        Torre torre = Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();
        return Piso.builder().id(1L).nroPiso(1).torre(torre).build();
    }

    private Activo buildActivo(Piso piso) {
        return Activo.builder()
                .id(UUID.randomUUID())
                .nro("DPTO 101")
                .tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.valueOf(80))
                .estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.valueOf(200000))
                .descripcion("Test")
                .piso(piso)
                .build();
    }

    @Test
    void saveFisico_asignaPisoYGuarda() {
        Piso piso = buildPiso();
        Activo activo = Activo.builder().nro("101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.ZERO).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.ZERO).descripcion("").build();

        when(pisoService.findById(1L)).thenReturn(piso);
        when(activoRepository.save(activo)).thenReturn(activo);

        Activo result = service.saveFisico(1L, activo);

        assertThat(result.getPiso()).isEqualTo(piso);
        verify(activoRepository).save(activo);
    }

    @Test
    void saveIndividual_delegaEnSaveFisico() {
        Piso piso = buildPiso();
        Activo activo = buildActivo(piso);
        activo.setPiso(null);

        when(pisoService.findById(1L)).thenReturn(piso);
        when(activoRepository.save(activo)).thenReturn(activo);

        Activo result = service.saveIndividual(1L, activo);

        assertThat(result).isNotNull();
        verify(pisoService).findById(1L);
    }

    @Test
    void save_delegaEnRepository() {
        Piso piso = buildPiso();
        Activo activo = buildActivo(piso);
        when(activoRepository.save(activo)).thenReturn(activo);

        Activo result = service.save(activo);

        assertThat(result).isEqualTo(activo);
    }

    @Test
    void deleteById_llamaRepository() {
        UUID id = UUID.randomUUID();
        service.deleteById(id);
        verify(activoRepository).deleteById(id);
    }

    @Test
    void findById_encontrado_devuelveActivo() {
        Piso piso = buildPiso();
        Activo activo = buildActivo(piso);
        when(activoRepository.findById(activo.getId())).thenReturn(Optional.of(activo));

        Activo result = service.findById(activo.getId());

        assertThat(result).isEqualTo(activo);
    }

    @Test
    void findById_noEncontrado_lanzaException() {
        UUID id = UUID.randomUUID();
        when(activoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activo no encontrado");
    }

    @Test
    void findByPiso_sinSearch_devuelveListaPorPiso() {
        Piso piso = buildPiso();
        Activo activo = buildActivo(piso);
        when(activoRepository.findByPisoId(1L)).thenReturn(List.of(activo));

        List<Activo> result = service.findByPiso(1L, null);

        assertThat(result).hasSize(1);
        verify(activoRepository).findByPisoId(1L);
    }

    @Test
    void findByPiso_conSearch_devuelveFiltrado() {
        Piso piso = buildPiso();
        Activo activo = buildActivo(piso);
        when(activoRepository.findByPisoIdAndNroContainingIgnoreCase(1L, "101"))
                .thenReturn(List.of(activo));

        List<Activo> result = service.findByPiso(1L, "101");

        assertThat(result).hasSize(1);
        verify(activoRepository).findByPisoIdAndNroContainingIgnoreCase(1L, "101");
    }

    @Test
    void listarPorProyectoYEstado_sinEstado_buscaTodos() {
        UUID idProyecto = UUID.randomUUID();
        Piso piso = buildPiso();
        Activo activo = buildActivo(piso);
        Page<Activo> page = new PageImpl<>(List.of(activo));
        when(activoRepository.findByPisoTorreProyectoId(eq(idProyecto), any())).thenReturn(page);

        Page<ActivoResponseDTO> result = service.listarPorProyectoYEstado(idProyecto, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listarPorProyectoYEstado_conEstado_filtraPorEstado() {
        UUID idProyecto = UUID.randomUUID();
        Piso piso = buildPiso();
        Activo activo = buildActivo(piso);
        Page<Activo> page = new PageImpl<>(List.of(activo));
        when(activoRepository.findByPisoTorreProyectoIdAndEstadoComercial(
                eq(idProyecto), eq(EstadoComercialActivo.DISPONIBLE), any())).thenReturn(page);

        Page<ActivoResponseDTO> result = service.listarPorProyectoYEstado(
                idProyecto, EstadoComercialActivo.DISPONIBLE, 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }
}
