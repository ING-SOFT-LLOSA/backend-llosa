package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.repository.PisoRepository;
import com.llosa.backend.proyecto.service.impl.PisoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PisoServiceTest {

    @Mock PisoRepository pisoRepository;
    @Mock TorreService torreService;
    @Mock HidratationService hidratationService;

    @InjectMocks PisoServiceImpl service;

    @Test
    void findById_encontrado_devuelvePiso() {
        Piso piso = Piso.builder().id(1L).nroPiso(3).build();
        when(pisoRepository.findById(1L)).thenReturn(Optional.of(piso));

        Piso result = service.findById(1L);

        assertThat(result).isEqualTo(piso);
    }

    @Test
    void findById_noEncontrado_lanzaException() {
        when(pisoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Piso no encontrado");
    }

    @Test
    void save_asignaTorreHidrataYGuarda() {
        Torre torre = Torre.builder().id(1L).nombre("Torre A").build();
        Piso piso = Piso.builder().nroPiso(1).build();
        Piso saved = Piso.builder().id(1L).nroPiso(1).torre(torre).build();

        when(torreService.findById(1L)).thenReturn(torre);
        when(pisoRepository.save(piso)).thenReturn(saved);

        Piso result = service.save(1L, piso);

        assertThat(result).isEqualTo(saved);
        assertThat(piso.getTorre()).isEqualTo(torre);
        verify(hidratationService).hydrateFloorMilestones(1L);
    }

    @Test
    void findByTorre_sinSearch_devuelveTodos() {
        Piso piso = Piso.builder().id(1L).nroPiso(1).build();
        when(pisoRepository.findByTorreId(1L)).thenReturn(List.of(piso));

        List<Piso> result = service.findByTorre(1L, null);

        assertThat(result).hasSize(1);
        verify(pisoRepository).findByTorreId(1L);
    }

    @Test
    void findByTorre_searchBlanco_devuelveTodos() {
        when(pisoRepository.findByTorreId(1L)).thenReturn(List.of());

        service.findByTorre(1L, "");

        verify(pisoRepository).findByTorreId(1L);
    }

    @Test
    void findByTorre_conSearch_devuelveFiltrado() {
        Piso piso = Piso.builder().id(1L).nroPiso(3).build();
        when(pisoRepository.findByTorreIdAndSearch(1L, "3")).thenReturn(List.of(piso));

        List<Piso> result = service.findByTorre(1L, "3");

        assertThat(result).hasSize(1);
        verify(pisoRepository).findByTorreIdAndSearch(1L, "3");
    }
}
