package com.llosa.backend.documentos.service;

import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.ReporteRepository;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del resolvedor polimórfico de entidades. Verifica que un
 * UUID se mapee a la entidad correcta del módulo proyecto siguiendo el orden de
 * prioridad definido y que un UUID huérfano sea rechazado.
 */
@ExtendWith(MockitoExtension.class)
class EntidadResolverServiceTest {

    @Mock ProyectoRepository proyectoRepository;
    @Mock ActivoRepository activoRepository;
    @Mock UsuarioActivoRepository usuarioActivoRepository;
    @Mock HitoRepository hitoRepository;
    @Mock HitoPisoRepository hitoPisoRepository;
    @Mock ReporteRepository reporteRepository;

    @InjectMocks EntidadResolverService service;

    @Test
    void resolver_proyecto() {
        UUID id = UUID.randomUUID();
        when(proyectoRepository.existsById(id)).thenReturn(true);
        assertThat(service.resolverEntidad(id)).isEqualTo("PROYECTO");
    }

    @Test
    void resolver_activo() {
        UUID id = UUID.randomUUID();
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(true);
        assertThat(service.resolverEntidad(id)).isEqualTo("ACTIVO");
    }

    @Test
    void resolver_usuarioActivo() {
        UUID id = UUID.randomUUID();
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(true);
        assertThat(service.resolverEntidad(id)).isEqualTo("USUARIO_ACTIVO");
    }

    @Test
    void resolver_hito() {
        UUID id = UUID.randomUUID();
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(true);
        assertThat(service.resolverEntidad(id)).isEqualTo("HITO");
    }

    @Test
    void resolver_hitoPiso() {
        UUID id = UUID.randomUUID();
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(false);
        when(hitoPisoRepository.existsById(id)).thenReturn(true);
        assertThat(service.resolverEntidad(id)).isEqualTo("HITO_PISO");
    }

    @Test
    void resolver_reporte() {
        UUID id = UUID.randomUUID();
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(false);
        when(hitoPisoRepository.existsById(id)).thenReturn(false);
        when(reporteRepository.existsById(id)).thenReturn(true);
        assertThat(service.resolverEntidad(id)).isEqualTo("REPORTE");
    }

    @Test
    void resolver_uuidHuerfano_lanzaBusinessException() {
        UUID id = UUID.randomUUID();
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(false);
        when(hitoPisoRepository.existsById(id)).thenReturn(false);
        when(reporteRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.resolverEntidad(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no corresponde a ninguna entidad");
    }
}
