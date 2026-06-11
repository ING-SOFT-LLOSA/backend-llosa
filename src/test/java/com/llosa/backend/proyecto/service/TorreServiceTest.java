package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.TorreRepository;
import com.llosa.backend.proyecto.service.impl.TorreServiceImpl;
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
class TorreServiceTest {

    @Mock TorreRepository torreRepository;
    @Mock ProyectoRepository proyectoRepository;

    @InjectMocks TorreServiceImpl service;

    private Proyecto buildProyecto() {
        return Proyecto.builder().id(UUID.randomUUID()).nombre("Proyecto Test").build();
    }

    @Test
    void save_asignaProyectoYGuarda() {
        Proyecto proyecto = buildProyecto();
        Torre torre = Torre.builder().nombre("Torre A").build();
        Torre saved = Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();

        when(proyectoRepository.findById(proyecto.getId())).thenReturn(Optional.of(proyecto));
        when(torreRepository.save(torre)).thenReturn(saved);

        Torre result = service.save(proyecto.getId(), torre);

        assertThat(result).isEqualTo(saved);
        assertThat(torre.getProyecto()).isEqualTo(proyecto);
    }

    @Test
    void save_proyectoNoEncontrado_lanzaException() {
        UUID idProyecto = UUID.randomUUID();
        when(proyectoRepository.findById(idProyecto)).thenReturn(Optional.empty());

        Torre torre = Torre.builder().nombre("Torre B").build();

        assertThatThrownBy(() -> service.save(idProyecto, torre))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Proyecto no encontrado");
    }

    @Test
    void findById_encontrado_devuelveTorre() {
        Torre torre = Torre.builder().id(1L).nombre("Torre A").build();
        when(torreRepository.findById(1L)).thenReturn(Optional.of(torre));

        Torre result = service.findById(1L);

        assertThat(result).isEqualTo(torre);
    }

    @Test
    void findById_noEncontrado_lanzaException() {
        when(torreRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Torre no encontrado");
    }

    @Test
    void findByProyecto_sinSearch_devuelveTodas() {
        UUID proyectoId = UUID.randomUUID();
        Torre torre = Torre.builder().id(1L).nombre("Torre A").build();
        when(torreRepository.findByProyectoId(proyectoId)).thenReturn(List.of(torre));

        List<Torre> result = service.findByProyecto(proyectoId, null);

        assertThat(result).hasSize(1);
        verify(torreRepository).findByProyectoId(proyectoId);
    }

    @Test
    void findByProyecto_searchBlanco_devuelveTodas() {
        UUID proyectoId = UUID.randomUUID();
        when(torreRepository.findByProyectoId(proyectoId)).thenReturn(List.of());

        service.findByProyecto(proyectoId, "  ");

        verify(torreRepository).findByProyectoId(proyectoId);
    }

    @Test
    void findByProyecto_conSearch_devuelveFiltrada() {
        UUID proyectoId = UUID.randomUUID();
        Torre torre = Torre.builder().id(1L).nombre("Torre A").build();
        when(torreRepository.findByProyectoIdAndNombreContainingIgnoreCase(proyectoId, "A"))
                .thenReturn(List.of(torre));

        List<Torre> result = service.findByProyecto(proyectoId, "A");

        assertThat(result).hasSize(1);
    }
}
