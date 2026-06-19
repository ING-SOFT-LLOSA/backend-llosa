package com.llosa.backend.pagos.service;

import com.llosa.backend.agenda.service.AgendaService;
import com.llosa.backend.comercial.service.RequisitoDocumentalService;
import com.llosa.backend.config.TestDataPagos;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.exception.EntidadDuplicadaException;
import com.llosa.backend.exception.EstadoInvalidoException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.pagos.dto.PagoRequest;
import com.llosa.backend.pagos.dto.PagoResponse;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.pagos.entity.Pago;
import com.llosa.backend.pagos.repository.CronogramaPagoRepository;
import com.llosa.backend.pagos.repository.PagoRepository;
import com.llosa.backend.pagos.service.impl.PagoServiceImpl;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    PagoRepository pagoRepository;

    @Mock
    CronogramaPagoRepository cronogramaPagoRepository;

    @Mock
    DocumentoService documentoService;

    @Mock
    DocumentoRepository documentoRepository;

    @Mock
    RequisitoDocumentalService requisitoDocumentalService;

    @Mock
    AgendaService agendaService;

    @InjectMocks
    PagoServiceImpl pagoService;

    @Test
    void listarPorCronograma_exitoso() {
        UUID uuidCp = UUID.randomUUID();
        var cp = CronogramaPago.builder().id(uuidCp).build();
        var pago = TestDataPagos.pago(cp, 1);

        when(pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(uuidCp))
                .thenReturn(List.of(pago));

        List<PagoResponse> result = pagoService.listarPorCronograma(uuidCp);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nroCuota()).isEqualTo(1);
    }

    @Test
    void listarPorCronograma_vacio_devuelveListaVacia() {
        UUID uuidCp = UUID.randomUUID();
        when(pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(uuidCp))
                .thenReturn(List.of());

        assertThat(pagoService.listarPorCronograma(uuidCp)).isEmpty();
    }

    @Test
    void agregarCuota_exitoso() {
        UUID uuidCp = UUID.randomUUID();
        var cp = CronogramaPago.builder().id(uuidCp).build();
        var request = TestDataPagos.crearPagoRequest(1);

        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.of(cp));
        when(pagoRepository.findByCronograma_IdAndNroCuota(uuidCp, 1)).thenReturn(Optional.empty());

        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        when(pagoRepository.save(captor.capture())).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PagoResponse result = pagoService.agregarCuota(uuidCp, request);

        assertThat(result.nroCuota()).isEqualTo(1);
        assertThat(result.montoProgramado()).isEqualByComparingTo(request.montoProgramado());
        assertThat(result.estado()).isEqualTo("PENDIENTE");
    }

    @Test
    void agregarCuota_cronogramaNoExiste_lanzaRecursoNoEncontrado() {
        UUID uuidCp = UUID.randomUUID();
        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.agregarCuota(uuidCp, TestDataPagos.crearPagoRequest(1)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Cronograma no encontrado");
    }

    @Test
    void agregarCuota_nroCuotaDuplicado_lanzaEntidadDuplicadaException() {
        UUID uuidCp = UUID.randomUUID();
        var cp = CronogramaPago.builder().id(uuidCp).build();
        var request = TestDataPagos.crearPagoRequest(1);

        when(cronogramaPagoRepository.findById(uuidCp)).thenReturn(Optional.of(cp));
        when(pagoRepository.findByCronograma_IdAndNroCuota(uuidCp, 1)).thenReturn(Optional.of(mock(Pago.class)));

        assertThatThrownBy(() -> pagoService.agregarCuota(uuidCp, request))
                .isInstanceOf(EntidadDuplicadaException.class)
                .hasMessageContaining("Ya existe una cuota con el número 1");
    }

    @Test
    void actualizarCuota_exitoso() {
        UUID uuidPago = UUID.randomUUID();
        var cp = CronogramaPago.builder().id(UUID.randomUUID()).build();
        var pago = TestDataPagos.pago(cp, 1);
        pago.setId(uuidPago);
        var request = new PagoRequest(2, new BigDecimal("30000.00"), LocalDate.now().plusMonths(2), null, null);

        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.of(pago));
        when(pagoRepository.findByCronograma_IdAndNroCuota(cp.getId(), 2)).thenReturn(Optional.empty());
        when(pagoRepository.save(any())).thenReturn(pago);

        PagoResponse result = pagoService.actualizarCuota(uuidPago, request);

        assertThat(result.nroCuota()).isEqualTo(2);
        assertThat(pago.getNroCuota()).isEqualTo(2);
        assertThat(pago.getMontoProgramado()).isEqualByComparingTo(new BigDecimal("30000.00"));
    }

    @Test
    void actualizarCuota_noExiste_lanzaRecursoNoEncontrado() {
        UUID uuidPago = UUID.randomUUID();
        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.actualizarCuota(uuidPago, TestDataPagos.crearPagoRequest(1)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Pago no encontrado");
    }

    @Test
    void actualizarCuota_nroCuotaDuplicado_lanzaEstadoEntidadDuplicadaException() {
        UUID uuidPago = UUID.randomUUID();
        UUID uuidCp = UUID.randomUUID();
        var cp = CronogramaPago.builder().id(uuidCp).build();
        var pago = TestDataPagos.pago(cp, 1);
        pago.setId(uuidPago);

        var request = new PagoRequest(2, new BigDecimal("30000.00"), LocalDate.now().plusMonths(2), null, null);

        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.of(pago));
        when(pagoRepository.findByCronograma_IdAndNroCuota(uuidCp, 2)).thenReturn(Optional.of(mock(Pago.class)));

        assertThatThrownBy(() -> pagoService.actualizarCuota(uuidPago, request))
                .isInstanceOf(EntidadDuplicadaException.class)
                .hasMessageContaining("Ya existe una cuota con el número 2");
    }

    @Test
    void eliminarCuota_exitoso() {
        UUID uuidPago = UUID.randomUUID();
        var cp = CronogramaPago.builder().id(UUID.randomUUID()).build();
        var pago = TestDataPagos.pago(cp, 1);
        pago.setId(uuidPago);

        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.of(pago));

        pagoService.eliminarCuota(uuidPago);

        verify(pagoRepository).deleteById(uuidPago);
    }

    @Test
    void eliminarCuota_noExiste_lanzaRecursoNoEncontrado() {
        UUID uuidPago = UUID.randomUUID();
        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.eliminarCuota(uuidPago))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Pago no encontrado");

        verify(pagoRepository, never()).deleteById(any());
    }

    @Test
    void cambiarEstado_aPAGADO_fijaFechaPagoYMonto() {
        UUID uuidPago = UUID.randomUUID();
        var cp = CronogramaPago.builder().id(UUID.randomUUID()).build();
        var pago = TestDataPagos.pago(cp, 1);
        pago.setId(uuidPago);

        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any())).thenReturn(pago);

        PagoResponse result = pagoService.cambiarEstado(uuidPago, "PAGADO", 1);

        assertThat(result.estado()).isEqualTo("PAGADO");
        assertThat(pago.getFechaPago()).isNotNull();
        assertThat(pago.getMontoPagado()).isEqualByComparingTo(pago.getMontoProgramado());
        assertThat(pago.getActualizadoPor()).isEqualTo(1);
    }

    @Test
    void cambiarEstado_aPAGADO_conMontoExistente_noSobrescribe() {
        UUID uuidPago = UUID.randomUUID();
        var cp = CronogramaPago.builder().id(UUID.randomUUID()).build();
        var pago = TestDataPagos.pago(cp, 1);
        pago.setId(uuidPago);
        pago.setMontoPagado(new BigDecimal("10000.00"));

        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any())).thenReturn(pago);

        pagoService.cambiarEstado(uuidPago, "PAGADO", 1);

        assertThat(pago.getMontoPagado()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void cambiarEstado_aVENCIDO_limpiaFechaPago() {
        UUID uuidPago = UUID.randomUUID();
        var cp = CronogramaPago.builder().id(UUID.randomUUID()).build();
        var pago = TestDataPagos.pago(cp, 1);
        pago.setId(uuidPago);
        pago.setFechaPago(java.time.LocalDateTime.now());

        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any())).thenReturn(pago);

        pagoService.cambiarEstado(uuidPago, "VENCIDO", 1);

        assertThat(pago.getFechaPago()).isNull();
    }

    @Test
    void cambiarEstado_estadoInvalido_lanzaBusinessException() {
        UUID uuidPago = UUID.randomUUID();
        var cp = CronogramaPago.builder().id(UUID.randomUUID()).build();
        var pago = TestDataPagos.pago(cp, 1);
        pago.setId(uuidPago);

        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.of(pago));

        assertThatThrownBy(() -> pagoService.cambiarEstado(uuidPago, "INVALIDO", 1))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("Estado inválido");
    }

    @Test
    void cambiarEstado_noExiste_lanzaRecursoNoEncontrado() {
        UUID uuidPago = UUID.randomUUID();
        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.cambiarEstado(uuidPago, "PAGADO", 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Pago no encontrado");
    }

    @Test
    void subirComprobante_exitoso() {
        UUID uuidPago = UUID.randomUUID();
        UUID uuidUa = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuidUa).build();
        var cp = CronogramaPago.builder().id(UUID.randomUUID()).usuarioActivo(ua).build();
        var pago = TestDataPagos.pago(cp, 1);
        pago.setId(uuidPago);
        pago.setEstado("PENDIENTE");

        MultipartFile file = mock(MultipartFile.class);
        DocumentoResponse docResponse = new DocumentoResponse(docId, null, null, null, null, null, null, null);

        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.of(pago));
        when(documentoService.subirDocumentoPolimorfico(
                eq(file), eq(TipoDocumento.COMPROBANTE),
                eq(uuidPago.toString()), eq("PAGO"), eq(1)))
                .thenReturn(docResponse);
        when(pagoRepository.save(any())).thenReturn(pago);

        PagoResponse result = pagoService.subirComprobante(uuidPago, file, 1, null);

        assertThat(result.uuidComprobante()).isEqualTo(docId);
        assertThat(pago.getEstado()).isEqualTo("PAGADO");
        assertThat(pago.getFechaPago()).isNotNull();
    }

    @Test
    void subirComprobante_pagoYaPagado_noCambiaEstado() {
        UUID uuidPago = UUID.randomUUID();
        UUID uuidUa = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuidUa).build();
        var cp = CronogramaPago.builder().id(UUID.randomUUID()).usuarioActivo(ua).build();
        var pago = Pago.builder()
                .id(uuidPago).cronograma(cp).nroCuota(1)
                .montoProgramado(new BigDecimal("25000.00"))
                .fechaVencimiento(LocalDate.now())
                .estado("PAGADO")
                .montoPagado(new BigDecimal("25000.00"))
                .fechaPago(java.time.LocalDateTime.now())
                .build();

        MultipartFile file = mock(MultipartFile.class);
        DocumentoResponse docResponse = new DocumentoResponse(docId, null, null, null, null, null, null, null);

        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.of(pago));
        when(documentoService.subirDocumentoPolimorfico(
                eq(file), eq(TipoDocumento.COMPROBANTE),
                eq(uuidPago.toString()), eq("PAGO"), eq(1)))
                .thenReturn(docResponse);
        when(pagoRepository.save(any())).thenReturn(pago);

        PagoResponse result = pagoService.subirComprobante(uuidPago, file, 1, null);

        assertThat(result.uuidComprobante()).isEqualTo(docId);
        assertThat(pago.getEstado()).isEqualTo("PAGADO");
    }

    @Test
    void subirComprobante_pagoNoExiste_lanzaRecursoNoEncontrado() {
        UUID uuidPago = UUID.randomUUID();
        when(pagoRepository.findById(uuidPago)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.subirComprobante(uuidPago, mock(MultipartFile.class), 1, null))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Pago no encontrado");
    }
}
