package com.llosa.backend.documentos.service;

import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.pagos.repository.CronogramaPagoRepository;
import com.llosa.backend.pagos.repository.PagoRepository;
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

@ExtendWith(MockitoExtension.class)
class EntidadResolverServiceTest {

    @Mock ProyectoRepository proyectoRepository;
    @Mock ActivoRepository activoRepository;
    @Mock UsuarioActivoRepository usuarioActivoRepository;
    @Mock HitoRepository hitoRepository;
    @Mock HitoPisoRepository hitoPisoRepository;
    @Mock ReporteRepository reporteRepository;
    @Mock RequisitoDocumentalRepository requisitoDocumentalRepository;
    @Mock PagoRepository pagoRepository;
    @Mock CronogramaPagoRepository cronogramaPagoRepository;

    @InjectMocks EntidadResolverService entidadResolverService;

    private final UUID id = UUID.randomUUID();

    @Test
    void resolverEntidad_PROYECTO() {
        when(proyectoRepository.existsById(id)).thenReturn(true);
        assertThat(entidadResolverService.resolverEntidad(id)).isEqualTo("PROYECTO");
    }

    @Test
    void resolverEntidad_ACTIVO() {
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(true);
        assertThat(entidadResolverService.resolverEntidad(id)).isEqualTo("ACTIVO");
    }

    @Test
    void resolverEntidad_USUARIO_ACTIVO() {
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(true);
        assertThat(entidadResolverService.resolverEntidad(id)).isEqualTo("USUARIO_ACTIVO");
    }

    @Test
    void resolverEntidad_HITO() {
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(true);
        assertThat(entidadResolverService.resolverEntidad(id)).isEqualTo("HITO");
    }

    @Test
    void resolverEntidad_HITO_PISO() {
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(false);
        when(hitoPisoRepository.existsById(id)).thenReturn(true);
        assertThat(entidadResolverService.resolverEntidad(id)).isEqualTo("HITO_PISO");
    }

    @Test
    void resolverEntidad_REPORTE() {
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(false);
        when(hitoPisoRepository.existsById(id)).thenReturn(false);
        when(reporteRepository.existsById(id)).thenReturn(true);
        assertThat(entidadResolverService.resolverEntidad(id)).isEqualTo("REPORTE");
    }

    @Test
    void resolverEntidad_REQUISITO_DOCUMENTAL() {
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(false);
        when(hitoPisoRepository.existsById(id)).thenReturn(false);
        when(reporteRepository.existsById(id)).thenReturn(false);
        when(requisitoDocumentalRepository.existsById(id)).thenReturn(true);
        assertThat(entidadResolverService.resolverEntidad(id)).isEqualTo("REQUISITO_DOCUMENTAL");
    }

    @Test
    void resolverEntidad_CRONOGRAMA_PAGO() {
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(false);
        when(hitoPisoRepository.existsById(id)).thenReturn(false);
        when(reporteRepository.existsById(id)).thenReturn(false);
        when(requisitoDocumentalRepository.existsById(id)).thenReturn(false);
        when(cronogramaPagoRepository.existsById(id)).thenReturn(true);
        assertThat(entidadResolverService.resolverEntidad(id)).isEqualTo("CRONOGRAMA_PAGO");
    }

    @Test
    void resolverEntidad_PAGO() {
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(false);
        when(hitoPisoRepository.existsById(id)).thenReturn(false);
        when(reporteRepository.existsById(id)).thenReturn(false);
        when(requisitoDocumentalRepository.existsById(id)).thenReturn(false);
        when(cronogramaPagoRepository.existsById(id)).thenReturn(false);
        when(pagoRepository.existsById(id)).thenReturn(true);
        assertThat(entidadResolverService.resolverEntidad(id)).isEqualTo("PAGO");
    }

    @Test
    void resolverEntidad_noCoincide_lanzaBusinessException() {
        when(proyectoRepository.existsById(id)).thenReturn(false);
        when(activoRepository.existsById(id)).thenReturn(false);
        when(usuarioActivoRepository.existsById(id)).thenReturn(false);
        when(hitoRepository.existsById(id)).thenReturn(false);
        when(hitoPisoRepository.existsById(id)).thenReturn(false);
        when(reporteRepository.existsById(id)).thenReturn(false);
        when(requisitoDocumentalRepository.existsById(id)).thenReturn(false);
        when(cronogramaPagoRepository.existsById(id)).thenReturn(false);
        when(pagoRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> entidadResolverService.resolverEntidad(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no corresponde a ninguna entidad");
    }
}
