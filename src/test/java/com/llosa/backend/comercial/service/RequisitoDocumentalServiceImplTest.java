package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EtapaRequisitoDocumental;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.comercial.service.impl.RequisitoDocumentalServiceImpl;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequisitoDocumentalServiceImplTest {

    @Mock RequisitoDocumentalRepository requisitoRepository;
    @Mock DocumentoRepository documentoRepository;
    @Mock DocumentoService documentoService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock HitoProcesoCompraRepository hitoComercialRepository;
    @Mock EtapaExpedienteRepository etapaExpedienteRepository;

    @InjectMocks RequisitoDocumentalServiceImpl requisitoDocumentalService;

    private final UUID requisitoId = UUID.randomUUID();
    private final UUID etapaId = UUID.randomUUID();

    private EtapaExpediente buildEtapa() {
        return EtapaExpediente.builder()
                .uuidEtapaExpediente(etapaId)
                .etapaProceso(EtapaProceso.SEPARACION)
                .build();
    }

    private RequisitoDocumental buildRequisito() {
        return RequisitoDocumental.builder()
                .id(requisitoId)
                .etapaExpediente(buildEtapa())
                .titulo("Requisito Test")
                .descripcion("Desc")
                .estado(EtapaRequisitoDocumental.PENDIENTE)
                .icono("description")
                .build();
    }

    private Usuario buildUsuario() {
        var u = new Usuario();
        u.setId(1);
        u.setFirebaseUuid("firebase-uid");
        u.setNombre("Test");
        return u;
    }

    @Test
    void asociarArchivoARequisito_exitoso() {
        // 1. Creamos un archivo simulado con los "Magic Bytes" de un PDF real (%PDF-1.4...)
        byte[] pdfBytes = "%PDF-1.4\n%...\n%%EOF".getBytes();
        var file = new MockMultipartFile(
                "file",
                "contrato.pdf",
                "application/pdf",
                pdfBytes
        );

        var requisito = buildRequisito();
        var usuario = buildUsuario();

        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.of(requisito));
        when(usuarioRepository.findByFirebaseUuid("firebase-uid")).thenReturn(Optional.of(usuario));
        when(requisitoRepository.save(any())).thenReturn(requisito);

        // Act
        var result = requisitoDocumentalService.asociarArchivoARequisito(requisitoId, file, "firebase-uid");

        // Assert
        assertThat(result.getEstado()).isEqualTo(EtapaRequisitoDocumental.COMPLETADO);
        assertThat(result.getFechaEmision()).isNotNull();

        // Verificamos usando el objeto 'file' real que creamos arriba
        verify(documentoService).subirDocumentoPolimorfico(file, TipoDocumento.PDF_LEGAL,
                requisitoId.toString(), "REQUISITO", 1);
    }

    @Test
    void asociarArchivoARequisito_requisitoNoExiste_lanzaEntityNotFound() {
        // 1. Creamos un archivo simulado con Magic Bytes válidos de PDF
        byte[] pdfBytes = "%PDF-1.4\n%...\n%%EOF".getBytes();
        var file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", pdfBytes);

        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.empty());

        // 2. Pasamos el 'file' simulado en lugar del mock vacío
        assertThatThrownBy(() -> requisitoDocumentalService.asociarArchivoARequisito(
                requisitoId, file, "firebase-uid"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void asociarArchivoARequisito_usuarioNoExiste_lanzaRecursoNoEncontrado() {
        // 1. Creamos un archivo simulado con Magic Bytes válidos de PDF
        byte[] pdfBytes = "%PDF-1.4\n%...\n%%EOF".getBytes();
        var file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", pdfBytes);

        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.of(buildRequisito()));
        when(usuarioRepository.findByFirebaseUuid("firebase-uid")).thenReturn(Optional.empty());

        // 2. Pasamos el 'file' simulado en lugar del mock vacío
        assertThatThrownBy(() -> requisitoDocumentalService.asociarArchivoARequisito(
                requisitoId, file, "firebase-uid"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarArchivoDeRequisito_exitoso() {
        var requisito = buildRequisito();
        requisito.setEstado(EtapaRequisitoDocumental.COMPLETADO);
        requisito.setFechaEmision(LocalDate.now());
        var usuario = buildUsuario();
        var documento = Documento.builder().id(UUID.randomUUID()).build();

        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.of(requisito));
        when(usuarioRepository.findByFirebaseUuid("firebase-uid")).thenReturn(Optional.of(usuario));
        when(documentoRepository.findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                "REQUISITO", requisitoId.toString())).thenReturn(Optional.of(documento));

        requisitoDocumentalService.eliminarArchivoDeRequisito(requisitoId, "firebase-uid");

        verify(documentoService).eliminarDocumento(documento.getId(), 1);
        assertThat(requisito.getEstado()).isEqualTo(EtapaRequisitoDocumental.PENDIENTE);
        assertThat(requisito.getFechaEmision()).isNull();
    }

    @Test
    void eliminarArchivoDeRequisito_requisitoNoExiste_lanzaEntityNotFound() {
        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitoDocumentalService.eliminarArchivoDeRequisito(requisitoId, "firebase-uid"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void eliminarArchivoDeRequisito_documentoNoExiste_lanzaEntityNotFound() {
        var requisito = buildRequisito();
        var usuario = buildUsuario();

        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.of(requisito));
        when(usuarioRepository.findByFirebaseUuid("firebase-uid")).thenReturn(Optional.of(usuario));
        when(documentoRepository.findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                "REQUISITO", requisitoId.toString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitoDocumentalService.eliminarArchivoDeRequisito(requisitoId, "firebase-uid"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void crearRequisito_exitoso() {
        var request = new RequisitoCreateRequest(etapaId, "Nuevo Requisito", "Desc", "Nota", null, "icon");

        when(etapaExpedienteRepository.findById(etapaId)).thenReturn(Optional.of(buildEtapa()));
        when(requisitoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = requisitoDocumentalService.crearRequisito(request);

        assertThat(result.getTitulo()).isEqualTo("Nuevo Requisito");
        assertThat(result.getEstado()).isEqualTo(EtapaRequisitoDocumental.PENDIENTE);
        assertThat(result.getIcono()).isEqualTo("icon");
    }

    @Test
    void crearRequisito_etapaNoExiste_lanzaEntityNotFound() {
        var request = new RequisitoCreateRequest(etapaId, "Requisito", null, null, null, null);
        when(etapaExpedienteRepository.findById(etapaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitoDocumentalService.crearRequisito(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void crearRequisito_conFechaEmision_usaEsaFecha() {
        var fecha = LocalDate.now();
        var request = new RequisitoCreateRequest(etapaId, "Requisito", null, null, fecha, null);

        when(etapaExpedienteRepository.findById(etapaId)).thenReturn(Optional.of(buildEtapa()));
        when(requisitoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = requisitoDocumentalService.crearRequisito(request);
        assertThat(result.getFechaEmision()).isEqualTo(fecha);
    }

    @Test
    void crearRequisito_iconoNull_usaDefault() {
        var request = new RequisitoCreateRequest(etapaId, "Requisito", null, null, null, null);

        when(etapaExpedienteRepository.findById(etapaId)).thenReturn(Optional.of(buildEtapa()));
        when(requisitoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = requisitoDocumentalService.crearRequisito(request);
        assertThat(result.getIcono()).isEqualTo("description");
    }

    @Test
    void actualizarRequisito_exitoso() {
        var requisito = buildRequisito();
        var request = new RequisitoUpdateRequest("Titulo Actualizado", "Nueva desc", "Nueva nota",
                EtapaRequisitoDocumental.COMPLETADO, null, "new_icon");

        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.of(requisito));
        when(requisitoRepository.save(any())).thenReturn(requisito);

        var result = requisitoDocumentalService.actualizarRequisito(requisitoId, request);

        assertThat(result.getTitulo()).isEqualTo("Titulo Actualizado");
        assertThat(result.getEstado()).isEqualTo(EtapaRequisitoDocumental.COMPLETADO);
    }

    @Test
    void actualizarRequisito_estadoNull_noCambiaEstado() {
        var requisito = buildRequisito();
        var request = new RequisitoUpdateRequest("Titulo", null, null, null, null, null);

        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.of(requisito));
        when(requisitoRepository.save(any())).thenReturn(requisito);

        var result = requisitoDocumentalService.actualizarRequisito(requisitoId, request);

        assertThat(result.getEstado()).isEqualTo(EtapaRequisitoDocumental.PENDIENTE);
    }

    @Test
    void actualizarRequisito_noExiste_lanzaEntityNotFound() {
        var request = new RequisitoUpdateRequest("Titulo", null, null, null, null, null);
        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitoDocumentalService.actualizarRequisito(requisitoId, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void eliminarRequisitoTotalmente_exitoso() {
        var requisito = buildRequisito();
        var usuario = buildUsuario();
        var documento = Documento.builder().id(UUID.randomUUID()).build();

        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.of(requisito));
        when(usuarioRepository.findByFirebaseUuid("firebase-uid")).thenReturn(Optional.of(usuario));
        when(documentoRepository.findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                "REQUISITO", requisitoId.toString())).thenReturn(Optional.of(documento));

        requisitoDocumentalService.eliminarRequisitoTotalmente(requisitoId, "firebase-uid");

        verify(documentoService).eliminarDocumento(documento.getId(), 1);
        verify(requisitoRepository).delete(requisito);
    }

    @Test
    void eliminarRequisitoTotalmente_sinDocumento_eliminaDirecto() {
        var requisito = buildRequisito();
        var usuario = buildUsuario();

        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.of(requisito));
        when(usuarioRepository.findByFirebaseUuid("firebase-uid")).thenReturn(Optional.of(usuario));
        when(documentoRepository.findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                "REQUISITO", requisitoId.toString())).thenReturn(Optional.empty());

        requisitoDocumentalService.eliminarRequisitoTotalmente(requisitoId, "firebase-uid");

        verify(documentoService, never()).eliminarDocumento(any(), anyInt());
        verify(requisitoRepository).delete(requisito);
    }

    @Test
    void eliminarRequisitoTotalmente_noExiste_lanzaEntityNotFound() {
        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitoDocumentalService.eliminarRequisitoTotalmente(requisitoId, "firebase-uid"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── completarRequisitoConDocumento ───────────────────────────────────────

    @Test
    void completarRequisitoConDocumento_conComentario_seteaNotaYCompleta() {
        var requisito = buildRequisito();
        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.of(requisito));

        requisitoDocumentalService.completarRequisitoConDocumento(
                requisitoId, "gs://bucket/file.pdf", "file.pdf", "application/pdf", 3, "Revisado OK");

        assertThat(requisito.getEstado()).isEqualTo(EtapaRequisitoDocumental.COMPLETADO);
        assertThat(requisito.getNotaCorporativa()).isEqualTo("Revisado OK");
        assertThat(requisito.getFechaEmision()).isNotNull();
        verify(documentoService).crearReferenciaDocumento(
                eq("gs://bucket/file.pdf"), eq("file.pdf"), eq("application/pdf"),
                eq(requisitoId.toString()), any(), eq(TipoDocumento.PDF_LEGAL), eq(3));
        verify(requisitoRepository).save(requisito);
    }

    @Test
    void completarRequisitoConDocumento_sinComentario_noSeteaNota() {
        var requisito = buildRequisito();
        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.of(requisito));

        requisitoDocumentalService.completarRequisitoConDocumento(
                requisitoId, "gs://bucket/file.pdf", "file.pdf", "application/pdf", 3, null);

        assertThat(requisito.getEstado()).isEqualTo(EtapaRequisitoDocumental.COMPLETADO);
        assertThat(requisito.getNotaCorporativa()).isNull();
        verify(requisitoRepository).save(requisito);
    }

    @Test
    void completarRequisitoConDocumento_noExiste_lanzaEntityNotFound() {
        when(requisitoRepository.findById(requisitoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitoDocumentalService.completarRequisitoConDocumento(
                requisitoId, "gs://b/f.pdf", "f.pdf", "application/pdf", 3, null))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
