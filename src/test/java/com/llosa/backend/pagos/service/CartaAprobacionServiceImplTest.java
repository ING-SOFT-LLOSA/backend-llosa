package com.llosa.backend.pagos.service;

import com.llosa.backend.config.TestDataPagos;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.EntidadDuplicadaException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.pagos.dto.CartaAprobacionResponse;
import com.llosa.backend.pagos.entity.CartaAprobacion;
import com.llosa.backend.pagos.repository.CartaAprobacionRepository;
import com.llosa.backend.pagos.service.impl.CartaAprobacionServiceImpl;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartaAprobacionServiceImplTest {

    @Mock
    CartaAprobacionRepository cartaAprobacionRepository;

    @Mock
    UsuarioActivoRepository usuarioActivoRepository;

    @Mock
    com.llosa.backend.comercial.repository.HitoProcesoCompraRepository hitoRepository;

    @Mock
    com.llosa.backend.comercial.service.HitoComercialService hitoComercialService;

    @InjectMocks
    CartaAprobacionServiceImpl cartaAprobacionService;

    @Test
    void crear_exitoso() {
        var request = TestDataPagos.crearCartaRequest();
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(request.uuidUsuarioActivo()).build();

        when(cartaAprobacionRepository.existsByUsuarioActivo_UuidUsuarioActivo(request.uuidUsuarioActivo()))
                .thenReturn(false);
        when(usuarioActivoRepository.findById(request.uuidUsuarioActivo()))
                .thenReturn(Optional.of(ua));

        ArgumentCaptor<CartaAprobacion> captor = ArgumentCaptor.forClass(CartaAprobacion.class);
        when(cartaAprobacionRepository.save(captor.capture())).thenAnswer(inv -> {
            CartaAprobacion ca = inv.getArgument(0);
            ca.setId(UUID.randomUUID());
            return ca;
        });

        CartaAprobacionResponse result = cartaAprobacionService.crear(request);

        assertThat(result.banco()).isEqualTo(request.banco());
        assertThat(result.montoAprobado()).isEqualByComparingTo(request.montoAprobado());
    }

    @Test
    void crear_duplicado_lanzaBusinessException() {
        var request = TestDataPagos.crearCartaRequest();
        when(cartaAprobacionRepository.existsByUsuarioActivo_UuidUsuarioActivo(request.uuidUsuarioActivo()))
                .thenReturn(true);

        assertThatThrownBy(() -> cartaAprobacionService.crear(request))
                .isInstanceOf(EntidadDuplicadaException.class)
                .hasMessageContaining("ya tiene una carta de aprobación");

        verify(usuarioActivoRepository, never()).findById(any());
    }

    @Test
    void crear_expedienteNoExiste_lanzaRecursoNoEncontrado() {
        var request = TestDataPagos.crearCartaRequest();
        when(cartaAprobacionRepository.existsByUsuarioActivo_UuidUsuarioActivo(request.uuidUsuarioActivo()))
                .thenReturn(false);
        when(usuarioActivoRepository.findById(request.uuidUsuarioActivo()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartaAprobacionService.crear(request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Expediente no encontrado");
    }

    @Test
    void obtenerPorUsuarioActivo_exitoso() {
        UUID uuidUa = UUID.randomUUID();
        var ca = CartaAprobacion.builder()
                .id(UUID.randomUUID())
                .usuarioActivo(UsuarioActivo.builder().uuidUsuarioActivo(uuidUa).build())
                .banco("Banco de Prueba")
                .build();

        when(cartaAprobacionRepository.findByUsuarioActivo_UuidUsuarioActivo(uuidUa))
                .thenReturn(Optional.of(ca));

        CartaAprobacionResponse result = cartaAprobacionService.obtenerPorUsuarioActivo(uuidUa);

        assertThat(result.banco()).isEqualTo("Banco de Prueba");
    }

    @Test
    void obtenerPorUsuarioActivo_noExiste_lanzaRecursoNoEncontrado() {
        UUID uuidUa = UUID.randomUUID();
        when(cartaAprobacionRepository.findByUsuarioActivo_UuidUsuarioActivo(uuidUa))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartaAprobacionService.obtenerPorUsuarioActivo(uuidUa))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("No hay carta de aprobación");
    }

    @Test
    void actualizar_exitoso() {
        UUID uuidCarta = UUID.randomUUID();
        var request = TestDataPagos.crearCartaRequest();
        var ca = CartaAprobacion.builder()
                .id(uuidCarta)
                .usuarioActivo(UsuarioActivo.builder().uuidUsuarioActivo(request.uuidUsuarioActivo()).build())
                .banco("Banco Viejo")
                .build();

        when(cartaAprobacionRepository.findById(uuidCarta)).thenReturn(Optional.of(ca));
        when(cartaAprobacionRepository.save(any())).thenReturn(ca);

        CartaAprobacionResponse result = cartaAprobacionService.actualizar(uuidCarta, request);

        assertThat(result.banco()).isEqualTo(request.banco());
        assertThat(ca.getBanco()).isEqualTo(request.banco());
    }

    @Test
    void actualizar_noExiste_lanzaRecursoNoEncontrado() {
        UUID uuidCarta = UUID.randomUUID();
        when(cartaAprobacionRepository.findById(uuidCarta)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartaAprobacionService.actualizar(uuidCarta, TestDataPagos.crearCartaRequest()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Carta de aprobación no encontrada");
    }

    @Test
    void eliminar_exitoso() {
        UUID uuidCarta = UUID.randomUUID();
        when(cartaAprobacionRepository.existsById(uuidCarta)).thenReturn(true);

        cartaAprobacionService.eliminar(uuidCarta);

        verify(cartaAprobacionRepository).deleteById(uuidCarta);
    }

    @Test
    void eliminar_noExiste_lanzaRecursoNoEncontrado() {
        UUID uuidCarta = UUID.randomUUID();
        when(cartaAprobacionRepository.existsById(uuidCarta)).thenReturn(false);

        assertThatThrownBy(() -> cartaAprobacionService.eliminar(uuidCarta))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Carta de aprobación no encontrada");

        verify(cartaAprobacionRepository, never()).deleteById(any());
    }
}
