package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.request.*;
import com.llosa.backend.proyecto.entity.*;
import com.llosa.backend.proyecto.enums.*;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.service.impl.ProyectoServiceImpl;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProyectoServiceTest {

    @Mock ProyectoRepository proyectoRepository;
    @Mock HitoPisoRepository hitoPisoRepository;
    @Mock TorreService torreService;
    @Mock ActivoService activoService;
    @Mock PisoService pisoService;
    @Mock HidratationService hidratacionService;
    @Mock com.llosa.backend.factory.FlujoConstruccionFactory flujoConstruccionFactory;

    @InjectMocks ProyectoServiceImpl service;

    private Proyecto buildProyecto() {
        return Proyecto.builder()
                .id(UUID.randomUUID())
                .nombre("Torre Sol")
                .descripcion("Proyecto test")
                .precertificacionEdgeLeed(false)
                .departamento("Lima")
                .distrito("Miraflores")
                .direccion("Av. Test 123")
                .build();
    }

    @Test
    void findById_encontrado_devuelveProyecto() {
        Proyecto p = buildProyecto();
        when(proyectoRepository.findById(p.getId())).thenReturn(Optional.of(p));

        Proyecto result = service.findById(p.getId());

        assertThat(result).isEqualTo(p);
    }

    @Test
    void findById_noEncontrado_lanzaEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(proyectoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Proyecto no encontrado");
    }

    @Test
    void findAll_sinSearch_devuelveTodos() {
        Proyecto p = buildProyecto();
        Pageable pageable = PageRequest.of(0, 10);
        // Envolvemos la lista en un PageImpl
        Page<Proyecto> pageMock = new PageImpl<>(List.of(p), pageable, 1);

        when(proyectoRepository.findAll(pageable)).thenReturn(pageMock);

        Page<Proyecto> result = service.findAll(null, pageable); // <-- Se pasa pageable

        assertThat(result.getContent()).hasSize(1); // <-- Validamos sobre el contenido de la página
        verify(proyectoRepository).findAll(pageable);
        verify(proyectoRepository, never())
                .findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(any(), any(), any());
    }

    @Test
    void findAll_searchBlanco_devuelveTodos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Proyecto> pageMock = new PageImpl<>(List.of(), pageable, 0);

        when(proyectoRepository.findAll(pageable)).thenReturn(pageMock);

        service.findAll("   ", pageable); // <-- Se pasa pageable

        verify(proyectoRepository).findAll(pageable);
    }

    @Test
    void findAll_conSearch_devuelveFiltrado() {
        Proyecto p = buildProyecto();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Proyecto> pageMock = new PageImpl<>(List.of(p), pageable, 1);

        // El repositorio ahora debe recibir el pageable como tercer argumento
        when(proyectoRepository.findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase("sol", "sol", pageable))
                .thenReturn(pageMock);

        Page<Proyecto> result = service.findAll("sol", pageable); // <-- Se pasa pageable

        assertThat(result.getContent()).hasSize(1);
        verify(proyectoRepository).findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase("sol", "sol", pageable);
    }

    @Test
    void getPorcentajeAvance_sinHitos_devuelveCero() {
        UUID id = UUID.randomUUID();
        when(hitoPisoRepository.countByProyectoId(id)).thenReturn(0L);

        double avance = service.getPorcentajeAvance(id);

        assertThat(avance).isEqualTo(0.0);
    }

    @Test
    void getPorcentajeAvance_conHitos_calculaPorcentaje() {
        UUID id = UUID.randomUUID();
        when(hitoPisoRepository.countByProyectoId(id)).thenReturn(10L);
        when(hitoPisoRepository.countByProyectoIdAndEstado(id, EstadoHito.COMPLETADO)).thenReturn(5L);

        double avance = service.getPorcentajeAvance(id);

        assertThat(avance).isEqualTo(50.0);
    }

    @Test
    void getPorcentajeAvance_todosCompletados_devuelveCien() {
        UUID id = UUID.randomUUID();
        when(hitoPisoRepository.countByProyectoId(id)).thenReturn(4L);
        when(hitoPisoRepository.countByProyectoIdAndEstado(id, EstadoHito.COMPLETADO)).thenReturn(4L);

        double avance = service.getPorcentajeAvance(id);

        assertThat(avance).isEqualTo(100.0);
    }

    @Test
    void deleteById_llamaRepository() {
        UUID id = UUID.randomUUID();
        service.deleteById(id);
        verify(proyectoRepository).deleteById(id);
    }

    @Test
    void cargarProyecto_estructuraCompleta_guardaTodo() {
        UUID idProyecto = UUID.randomUUID();
        Proyecto proyecto = buildProyecto();
        proyecto.setId(idProyecto);

        Torre torreGuardada = Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();
        Piso pisoGuardado = Piso.builder().id(1L).nroPiso(1).torre(torreGuardada).build();
        Activo activoGuardado = Activo.builder()
                .id(UUID.randomUUID()).nro("101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.valueOf(80)).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.valueOf(200000)).descripcion("Dpto 101").piso(pisoGuardado)
                .build();

        when(proyectoRepository.findById(idProyecto)).thenReturn(Optional.of(proyecto));
        when(torreService.save(eq(idProyecto), any(Torre.class))).thenReturn(torreGuardada);
        when(pisoService.save(eq(1L), any(Piso.class))).thenReturn(pisoGuardado);
        when(activoService.saveFisico(eq(1L), any(Activo.class))).thenReturn(activoGuardado);
        when(proyectoRepository.save(proyecto)).thenReturn(proyecto);

        ActivoRequestDTO activoDTO = new ActivoRequestDTO("101", TipoActivo.DEPARTAMENTO,
                BigDecimal.valueOf(80), BigDecimal.valueOf(80), EstadoComercialActivo.DISPONIBLE,
                BigDecimal.valueOf(200000), "Dpto 101", false);
        PisoRequestDTO pisoDTO = new PisoRequestDTO(1, List.of(activoDTO));
        TorreRequestDTO torreDTO = new TorreRequestDTO("Torre A", List.of(pisoDTO));
        ProyectoCargaDTO cargaDTO = new ProyectoCargaDTO(List.of(torreDTO));

        service.cargarProyecto(idProyecto, cargaDTO);

        verify(torreService).save(eq(idProyecto), any(Torre.class));
        verify(pisoService).save(eq(1L), any(Piso.class));
        verify(activoService).saveFisico(eq(1L), any(Activo.class));
        verify(proyectoRepository).save(proyecto);
    }

    @Test
    void save_nuevoProyecto_generaHitosPorDefecto() {
        Proyecto nuevo = Proyecto.builder().nombre("Nuevo").build(); // id == null
        Hito h1 = Hito.builder().titulo("Cimentación").estado(EstadoHito.PENDIENTE).build();
        when(flujoConstruccionFactory.generarHitosPorDefecto())
                .thenReturn(new java.util.ArrayList<>(List.of(h1)));
        when(proyectoRepository.save(nuevo)).thenReturn(nuevo);

        Proyecto result = service.save(nuevo);

        assertThat(result.getHitos()).hasSize(1);
        assertThat(result.getHitos().get(0).getProyecto()).isEqualTo(nuevo);
        verify(flujoConstruccionFactory).generarHitosPorDefecto();
    }

    @Test
    void save_proyectoExistente_noGeneraHitos() {
        Proyecto existente = buildProyecto(); // id != null
        when(proyectoRepository.save(existente)).thenReturn(existente);

        service.save(existente);

        verify(flujoConstruccionFactory, never()).generarHitosPorDefecto();
    }

    @Test
    void findHitosByProyecto_marcaCompletadoSiTodosHitoPisoCompletados() {
        UUID idProyecto = UUID.randomUUID();
        Proyecto proyecto = buildProyecto();
        proyecto.setId(idProyecto);

        HitoPiso hp = HitoPiso.builder().estado(EstadoHito.COMPLETADO).build();
        Hito hito = Hito.builder().titulo("Acabados").estado(EstadoHito.PENDIENTE)
                .hitosPiso(new java.util.ArrayList<>(List.of(hp))).build();
        proyecto.setHitos(new java.util.ArrayList<>(List.of(hito)));

        when(proyectoRepository.findById(idProyecto)).thenReturn(Optional.of(proyecto));

        List<Hito> result = service.findHitosByProyecto(idProyecto);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEstado()).isEqualTo(EstadoHito.COMPLETADO);
        assertThat(result.get(0).getFechaCompletado()).isNotNull();
    }

    @Test
    void findHitosByProyecto_noMarcaSiHitoPisoPendiente() {
        UUID idProyecto = UUID.randomUUID();
        Proyecto proyecto = buildProyecto();
        proyecto.setId(idProyecto);

        HitoPiso hp = HitoPiso.builder().estado(EstadoHito.PENDIENTE).build();
        Hito hito = Hito.builder().titulo("Acabados").estado(EstadoHito.PENDIENTE)
                .hitosPiso(new java.util.ArrayList<>(List.of(hp))).build();
        proyecto.setHitos(new java.util.ArrayList<>(List.of(hito)));

        when(proyectoRepository.findById(idProyecto)).thenReturn(Optional.of(proyecto));

        List<Hito> result = service.findHitosByProyecto(idProyecto);

        assertThat(result.get(0).getEstado()).isEqualTo(EstadoHito.PENDIENTE);
    }

    @Test
    void findHitosByProyecto_hitoYaCompletado_seOmite() {
        UUID idProyecto = UUID.randomUUID();
        Proyecto proyecto = buildProyecto();
        proyecto.setId(idProyecto);

        Hito hito = Hito.builder().titulo("Listo").estado(EstadoHito.COMPLETADO)
                .hitosPiso(new java.util.ArrayList<>()).build();
        proyecto.setHitos(new java.util.ArrayList<>(List.of(hito)));

        when(proyectoRepository.findById(idProyecto)).thenReturn(Optional.of(proyecto));

        List<Hito> result = service.findHitosByProyecto(idProyecto);

        assertThat(result.get(0).getEstado()).isEqualTo(EstadoHito.COMPLETADO);
    }

    @Test
    void cargarProyecto_proyectoNoEncontrado_lanzaException() {
        UUID idProyecto = UUID.randomUUID();
        when(proyectoRepository.findById(idProyecto)).thenReturn(Optional.empty());

        ProyectoCargaDTO cargaDTO = new ProyectoCargaDTO(List.of());

        assertThatThrownBy(() -> service.cargarProyecto(idProyecto, cargaDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Proyecto no encontrado");
    }
}
