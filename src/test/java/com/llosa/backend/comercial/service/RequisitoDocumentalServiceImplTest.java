package com.llosa.backend.comercial.service;

import com.llosa.backend.annotation.CP;
import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.comercial.service.impl.RequisitoDocumentalServiceImpl;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del ciclo de vida de los Requisitos Documentales del
 * proceso comercial (CP25). Valida que asociar un archivo marque el requisito
 * como COMPLETADA y que eliminarlo lo regrese a PENDIENTE, delegando la
 * persistencia física en el DocumentoService (Bóveda Digital / GCS).
 */
@ExtendWith(MockitoExtension.class)
class RequisitoDocumentalServiceImplTest {

    @Mock RequisitoDocumentalRepository requisitoRepository;
    @Mock DocumentoRepository documentoRepository;
    @Mock DocumentoService documentoService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock HitoProcesoCompraRepository hitoRepository;

    @InjectMocks RequisitoDocumentalServiceImpl service;

    private static final String FIREBASE_UID = "firebase-uid-123";

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(7);
        u.setFirebaseUuid(FIREBASE_UID);
        u.setTipoUsuario("ASESOR");
        return u;
    }

    private RequisitoDocumental requisito(UUID id, UUID uaId) {
        UsuarioActivo ua = UsuarioActivo.builder().uuidUsuarioActivo(uaId).build();
        HitoProcesoCompra hito = HitoProcesoCompra.builder()
                .uuidHitoComercial(UUID.randomUUID())
                .usuarioActivo(ua)
                .etapaProceso(EtapaProceso.CONTRATO)
                .nombreHito("Minuta")
                .orden(1)
                .build();
        return RequisitoDocumental.builder()
                .id(id)
                .hitoComercial(hito)
                .titulo("Minuta de compraventa")
                .estado("PENDIENTE")
                .build();
    }

    // ─── Asociar archivo ──────────────────────────────────────────────────────────

    @Test
    @CP(value = "CP25", scenario = "Asociar PDF al requisito",
            input = "requisitoId + minuta.pdf",
            expected = "Sube documento como PDF_LEGAL y marca requisito COMPLETADA con fecha de emisión")
    @DisplayName("asociarArchivoARequisito marca COMPLETADA y delega la subida a la Bóveda")
    void asociarArchivo_marcaCompletadaYDelegaSubida() {
        UUID reqId = UUID.randomUUID();
        UUID uaId = UUID.randomUUID();
        RequisitoDocumental req = requisito(reqId, uaId);

        when(requisitoRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(usuario()));
        when(requisitoRepository.save(any(RequisitoDocumental.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "minuta.pdf", "application/pdf", new byte[]{1, 2, 3});

        RequisitoDocumental resultado = service.asociarArchivoARequisito(reqId, pdf, FIREBASE_UID);

        assertThat(resultado.getEstado()).isEqualTo("COMPLETADA");
        assertThat(resultado.getFechaEmision()).isEqualTo(LocalDate.now());
        verify(documentoService).subirDocumentoPolimorfico(
                eq(uaId), eq(pdf), eq(TipoDocumento.PDF_LEGAL),
                eq(reqId.toString()), eq("REQUISITO"), eq(7));
    }

    @Test
    void asociarArchivo_requisitoInexistente_lanzaNotFound() {
        UUID reqId = UUID.randomUUID();
        when(requisitoRepository.findById(reqId)).thenReturn(Optional.empty());

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "x.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.asociarArchivoARequisito(reqId, pdf, FIREBASE_UID))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(documentoService);
    }

    @Test
    void asociarArchivo_usuarioInexistente_lanzaException() {
        UUID reqId = UUID.randomUUID();
        when(requisitoRepository.findById(reqId)).thenReturn(Optional.of(requisito(reqId, UUID.randomUUID())));
        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.empty());

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "x.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.asociarArchivoARequisito(reqId, pdf, FIREBASE_UID))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── Eliminar archivo ───────────────────────────────────────────────────────

    @Test
    @CP(value = "CP25", scenario = "Quitar archivo del requisito",
            input = "requisitoId con documento asociado",
            expected = "Elimina documento físico y regresa requisito a PENDIENTE")
    void eliminarArchivo_regresaAPendienteYBorraDocumento() {
        UUID reqId = UUID.randomUUID();
        RequisitoDocumental req = requisito(reqId, UUID.randomUUID());
        req.setEstado("COMPLETADA");
        req.setFechaEmision(LocalDate.now());

        UUID docId = UUID.randomUUID();
        Documento doc = Documento.builder().id(docId).build();

        when(requisitoRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(usuario()));
        when(documentoRepository.findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                "REQUISITO", reqId.toString())).thenReturn(Optional.of(doc));

        service.eliminarArchivoDeRequisito(reqId, FIREBASE_UID);

        assertThat(req.getEstado()).isEqualTo("PENDIENTE");
        assertThat(req.getFechaEmision()).isNull();
        verify(documentoService).eliminarDocumento(docId, 7);
        verify(requisitoRepository).save(req);
    }

    @Test
    void eliminarArchivo_sinDocumentoFisico_lanzaNotFound() {
        UUID reqId = UUID.randomUUID();
        when(requisitoRepository.findById(reqId)).thenReturn(Optional.of(requisito(reqId, UUID.randomUUID())));
        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(usuario()));
        when(documentoRepository.findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                "REQUISITO", reqId.toString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminarArchivoDeRequisito(reqId, FIREBASE_UID))
                .isInstanceOf(EntityNotFoundException.class);
        verify(documentoService, never()).eliminarDocumento(any(), any());
    }

    // ─── Crear ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("crearRequisito asocia al hito y arranca en estado PENDIENTE")
    void crearRequisito_creaPendienteConIconoPorDefecto() {
        UUID hitoId = UUID.randomUUID();
        HitoProcesoCompra hito = HitoProcesoCompra.builder()
                .uuidHitoComercial(hitoId).orden(1).nombreHito("Contrato")
                .etapaProceso(EtapaProceso.CONTRATO).build();
        when(hitoRepository.findById(hitoId)).thenReturn(Optional.of(hito));
        when(requisitoRepository.save(any(RequisitoDocumental.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RequisitoCreateRequest request = new RequisitoCreateRequest(
                hitoId, "DNI del titular", "Copia legible", null, null);

        RequisitoDocumental creado = service.crearRequisito(request);

        assertThat(creado.getEstado()).isEqualTo("PENDIENTE");
        assertThat(creado.getTitulo()).isEqualTo("DNI del titular");
        assertThat(creado.getIcono()).isEqualTo("description"); // valor por defecto
        assertThat(creado.getHitoComercial()).isEqualTo(hito);
    }

    @Test
    void crearRequisito_hitoInexistente_lanzaNotFound() {
        UUID hitoId = UUID.randomUUID();
        when(hitoRepository.findById(hitoId)).thenReturn(Optional.empty());

        RequisitoCreateRequest request = new RequisitoCreateRequest(
                hitoId, "Título", null, null, "folder");

        assertThatThrownBy(() -> service.crearRequisito(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── Actualizar ───────────────────────────────────────────────────────────────

    @Test
    void actualizarRequisito_actualizaCamposYEstado() {
        UUID reqId = UUID.randomUUID();
        RequisitoDocumental req = requisito(reqId, UUID.randomUUID());
        when(requisitoRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(requisitoRepository.save(any(RequisitoDocumental.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RequisitoUpdateRequest request = new RequisitoUpdateRequest(
                "Nuevo título", "Nueva desc", "Nota interna", "RECHAZADO");

        RequisitoDocumental actualizado = service.actualizarRequisito(reqId, request);

        assertThat(actualizado.getTitulo()).isEqualTo("Nuevo título");
        assertThat(actualizado.getEstado()).isEqualTo("RECHAZADO");
        assertThat(actualizado.getNotaCorporativa()).isEqualTo("Nota interna");
    }

    @Test
    void actualizarRequisito_estadoNulo_mantieneEstadoPrevio() {
        UUID reqId = UUID.randomUUID();
        RequisitoDocumental req = requisito(reqId, UUID.randomUUID());
        req.setEstado("COMPLETADA");
        when(requisitoRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(requisitoRepository.save(any(RequisitoDocumental.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RequisitoUpdateRequest request = new RequisitoUpdateRequest(
                "T", "D", "N", null);

        RequisitoDocumental actualizado = service.actualizarRequisito(reqId, request);

        assertThat(actualizado.getEstado()).isEqualTo("COMPLETADA");
    }

    // ─── Eliminar totalmente ────────────────────────────────────────────────────

    @Test
    void eliminarRequisitoTotalmente_conDocumento_borraDocumentoYRequisito() {
        UUID reqId = UUID.randomUUID();
        RequisitoDocumental req = requisito(reqId, UUID.randomUUID());
        Documento doc = Documento.builder().id(UUID.randomUUID()).build();

        when(requisitoRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(usuario()));
        when(documentoRepository.findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                "REQUISITO", reqId.toString())).thenReturn(Optional.of(doc));

        service.eliminarRequisitoTotalmente(reqId, FIREBASE_UID);

        verify(documentoService).eliminarDocumento(doc.getId(), 7);
        verify(requisitoRepository).delete(req);
    }

    @Test
    void eliminarRequisitoTotalmente_sinDocumento_soloBorraRequisito() {
        UUID reqId = UUID.randomUUID();
        RequisitoDocumental req = requisito(reqId, UUID.randomUUID());

        when(requisitoRepository.findById(reqId)).thenReturn(Optional.of(req));
        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(usuario()));
        when(documentoRepository.findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                "REQUISITO", reqId.toString())).thenReturn(Optional.empty());

        service.eliminarRequisitoTotalmente(reqId, FIREBASE_UID);

        verify(documentoService, never()).eliminarDocumento(any(), any());
        verify(requisitoRepository).delete(req);
    }
}
