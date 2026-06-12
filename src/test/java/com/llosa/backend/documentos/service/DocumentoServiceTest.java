package com.llosa.backend.documentos.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.llosa.backend.annotation.CP;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.dto.StageDocumentResponse;
import com.llosa.backend.documentos.dto.SubirDocumentoRequest;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.entity.TipoDocumentoConfig;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.repository.TipoDocumentoConfigRepository;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del módulo "Tracker de Documentos Legales (Bóveda Digital)".
 *
 * Cubre los casos de prueba CP25, CP26, CP27 y CP28 del documento de
 * especificación de casos de prueba. Las pruebas se construyen a partir de los
 * requisitos esperados del sistema (validación de formato/peso, generación de
 * Signed URL temporal, segregación de accesos), no a partir de la implementación.
 */
@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    private static final long MB = 1024L * 1024L;
    private static final String BUCKET = "llosa-test-bucket";

    @Mock DocumentoRepository documentoRepository;
    @Mock TipoDocumentoConfigRepository tipoDocumentoConfigRepository;
    @Mock EntidadResolverService entidadResolver;
    @Mock UsuarioActivoRepository usuarioActivoRepository;
    @Mock Storage storage;

    DocumentoService service;

    @BeforeEach
    void setUp() {
        // gcsBucketName es un String inyectado por constructor: se arma a mano.
        service = new DocumentoService(
                documentoRepository,
                tipoDocumentoConfigRepository,
                entidadResolver,
                usuarioActivoRepository,
                storage,
                BUCKET
        );
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private TipoDocumentoConfig configPdf20Mb() {
        return new TipoDocumentoConfig(
                TipoDocumento.PDF_LEGAL,
                "Documento legal en PDF",
                "application/pdf",
                20L * MB
        );
    }

    private Documento documentoGuardado(UUID id) {
        return Documento.builder()
                .id(id)
                .rutaGcs("proyectos/requisito/" + UUID.randomUUID() + "/x.pdf")
                .nombreOriginal("minuta.pdf")
                .idReferencia(UUID.randomUUID().toString())
                .entidadReferencia("REQUISITO")
                .tipoDocumento(TipoDocumento.PDF_LEGAL)
                .tipoMime("application/pdf")
                .accesoRestringido(true)
                .subidoPor(7)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── CP25: Carga obligatoria de PDF de respaldo en hito legal ────────────────

    @Nested
    @DisplayName("CP25 - Carga de hito legal con PDF de respaldo válido")
    class SubidaValida {

        @Test
        @CP(value = "CP25", scenario = "Hito legal con PDF válido",
                input = "minuta.pdf (application/pdf, 8MB) sobre entidad REQUISITO",
                expected = "Documento transferido a GCS, persistido y acceso restringido")
        void subirDocumentoPolimorfico_pdfValido_persisteYRestringeAcceso() {
            when(tipoDocumentoConfigRepository.findById(TipoDocumento.PDF_LEGAL))
                    .thenReturn(Optional.of(configPdf20Mb()));
            when(documentoRepository.save(any(Documento.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            byte[] contenido = new byte[(int) (8 * MB)]; // 8 MB → dentro del límite
            MockMultipartFile pdf = new MockMultipartFile(
                    "file", "minuta.pdf", "application/pdf", contenido);
            UUID uaId = UUID.randomUUID();
            UUID refId = UUID.randomUUID();

            DocumentoResponse resp = service.subirDocumentoPolimorfico(
                    uaId, pdf, TipoDocumento.PDF_LEGAL, refId.toString(), "REQUISITO", 7);

            assertThat(resp.nombreOriginal()).isEqualTo("minuta.pdf");
            assertThat(resp.tipoDocumento()).isEqualTo(TipoDocumento.PDF_LEGAL);
            assertThat(resp.entidadReferencia()).isEqualTo("REQUISITO");

            // Transfiere a GCS y registra el documento con acceso restringido.
            verify(storage).create(any(BlobInfo.class), any(byte[].class));
            ArgumentCaptor<Documento> captor = ArgumentCaptor.forClass(Documento.class);
            verify(documentoRepository).save(captor.capture());
            Documento guardado = captor.getValue();
            assertThat(guardado.isAccesoRestringido()).isTrue();
            assertThat(guardado.getRutaGcs()).contains("proyectos/requisito/");
            assertThat(guardado.getRutaGcs()).endsWith(".pdf");
        }

        @Test
        @CP(value = "CP25", scenario = "Subida genérica resuelve la entidad por UUID",
                input = "idReferencia que corresponde a un ACTIVO",
                expected = "El resolver determina la entidad y delega la subida")
        void subirDocumento_resuelveEntidadAutomaticamente() {
            UUID refId = UUID.randomUUID();
            when(entidadResolver.resolverEntidad(refId)).thenReturn("ACTIVO");
            when(tipoDocumentoConfigRepository.findById(TipoDocumento.PDF_LEGAL))
                    .thenReturn(Optional.of(configPdf20Mb()));
            when(documentoRepository.save(any(Documento.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MockMultipartFile pdf = new MockMultipartFile(
                    "file", "escritura.pdf", "application/pdf", new byte[]{1, 2, 3});

            DocumentoResponse resp = service.subirDocumento(
                    refId, pdf, new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL), 7);

            assertThat(resp.entidadReferencia()).isEqualTo("ACTIVO");
            verify(entidadResolver).resolverEntidad(refId);
        }
    }

    // ─── CP26: Rechazo por formato no PDF o peso excedido (>20 MB) ───────────────

    @Nested
    @DisplayName("CP26 - Rechazo de documento legal por formato/peso inválido")
    class ValidacionArchivo {

        @Test
        @CP(value = "CP26", scenario = "Formato no permitido",
                input = "contrato.docx (application/msword)",
                expected = "Rechaza por no ser PDF; no toca GCS ni BD")
        void subir_formatoNoPdf_lanzaBusinessException() {
            when(tipoDocumentoConfigRepository.findById(TipoDocumento.PDF_LEGAL))
                    .thenReturn(Optional.of(configPdf20Mb()));

            MockMultipartFile docx = new MockMultipartFile(
                    "file", "contrato.docx", "application/msword", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> service.subirDocumentoPolimorfico(
                    UUID.randomUUID(), docx, TipoDocumento.PDF_LEGAL,
                    UUID.randomUUID().toString(), "REQUISITO", 7))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no permitido");

            verifyNoInteractions(storage);
            verify(documentoRepository, never()).save(any());
        }

        @Test
        @CP(value = "CP26", scenario = "Peso excedido",
                input = "escritura_25MB.pdf",
                expected = "Supera el límite de 20 MB; rechaza la carga")
        void subir_pesoExcedido_lanzaBusinessException() {
            when(tipoDocumentoConfigRepository.findById(TipoDocumento.PDF_LEGAL))
                    .thenReturn(Optional.of(configPdf20Mb()));

            byte[] pesado = new byte[(int) (25 * MB)]; // 25 MB > 20 MB
            MockMultipartFile pdf = new MockMultipartFile(
                    "file", "escritura.pdf", "application/pdf", pesado);

            assertThatThrownBy(() -> service.subirDocumentoPolimorfico(
                    UUID.randomUUID(), pdf, TipoDocumento.PDF_LEGAL,
                    UUID.randomUUID().toString(), "REQUISITO", 7))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("20 MB");

            verifyNoInteractions(storage);
        }

        @Test
        @CP(value = "CP26", scenario = "Archivo vacío",
                input = "archivo de 0 bytes",
                expected = "Rechaza por archivo vacío")
        void subir_archivoVacio_lanzaBusinessException() {
            MockMultipartFile vacio = new MockMultipartFile(
                    "file", "vacio.pdf", "application/pdf", new byte[0]);

            assertThatThrownBy(() -> service.subirDocumentoPolimorfico(
                    UUID.randomUUID(), vacio, TipoDocumento.PDF_LEGAL,
                    UUID.randomUUID().toString(), "REQUISITO", 7))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("vacío");
        }

        @Test
        @CP(value = "CP26", scenario = "Tipo de documento sin configuración",
                input = "tipo sin registro en tipo_documento_config",
                expected = "Rechaza por tipo no configurado")
        void subir_tipoNoConfigurado_lanzaBusinessException() {
            when(tipoDocumentoConfigRepository.findById(TipoDocumento.PDF_LEGAL))
                    .thenReturn(Optional.empty());

            MockMultipartFile pdf = new MockMultipartFile(
                    "file", "x.pdf", "application/pdf", new byte[]{1});

            assertThatThrownBy(() -> service.subirDocumentoPolimorfico(
                    UUID.randomUUID(), pdf, TipoDocumento.PDF_LEGAL,
                    UUID.randomUUID().toString(), "REQUISITO", 7))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no configurado");
        }
    }

    // ─── CP27: Signed URL temporal (15 minutos) ──────────────────────────────────

    @Nested
    @DisplayName("CP27 - Generación de Signed URL temporal")
    class SignedUrl {

        @Test
        @CP(value = "CP27", scenario = "Visualización segura vía URL firmada",
                input = "documentoId existente",
                expected = "URL firmada V4 con expiración a 15 minutos")
        void generarSignedUrl_documentoExistente_devuelveUrlConExpiracion() throws Exception {
            UUID docId = UUID.randomUUID();
            when(documentoRepository.findById(docId))
                    .thenReturn(Optional.of(documentoGuardado(docId)));
            when(storage.signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES), any()))
                    .thenReturn(new URL("https://storage.googleapis.com/firmada?sig=abc"));

            Instant antes = Instant.now();
            SignedUrlResponse resp = service.generarSignedUrl(docId);

            assertThat(resp.url()).startsWith("https://");
            // La expiración debe ubicarse alrededor de 15 minutos en el futuro.
            assertThat(resp.expiracion()).isBetween(
                    antes.plusSeconds(14 * 60), antes.plusSeconds(16 * 60));
            verify(storage).signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES), any());
        }

        @Test
        @CP(value = "CP27", scenario = "Documento inexistente",
                input = "documentoId no registrado",
                expected = "EntityNotFoundException; no se firma URL")
        void generarSignedUrl_documentoInexistente_lanzaNotFound() {
            UUID docId = UUID.randomUUID();
            when(documentoRepository.findById(docId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generarSignedUrl(docId))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(storage, never()).signUrl(any(), anyLong(), any(), any());
        }

        @Test
        @CP(value = "CP27", scenario = "Sobrecarga con usuarioId delega en la principal",
                input = "documentoId + usuarioId",
                expected = "Mismo comportamiento que generarSignedUrl(documentoId)")
        void generarSignedUrl_conUsuarioId_delega() throws Exception {
            UUID docId = UUID.randomUUID();
            when(documentoRepository.findById(docId))
                    .thenReturn(Optional.of(documentoGuardado(docId)));
            when(storage.signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES), any()))
                    .thenReturn(new URL("https://storage.googleapis.com/firmada"));

            SignedUrlResponse resp = service.generarSignedUrl(docId, 99);

            assertThat(resp.url()).isNotBlank();
        }
    }

    // ─── CP28: Segregación / acceso restringido y listados ───────────────────────

    @Nested
    @DisplayName("CP28 - Segregación y recuperación de documentos por referencia")
    class ListadoYSegregacion {

        @Test
        @CP(value = "CP28", scenario = "Listado por referencia con tipo",
                input = "idReferencia + TipoDocumento.PDF_LEGAL",
                expected = "Filtra por entidad resuelta y tipo")
        void listar_conTipo_filtraPorTipo() {
            UUID refId = UUID.randomUUID();
            when(entidadResolver.resolverEntidad(refId)).thenReturn("USUARIO_ACTIVO");
            when(documentoRepository.findByIdReferenciaAndEntidadReferenciaAndTipoDocumento(
                    refId.toString(), "USUARIO_ACTIVO", TipoDocumento.PDF_LEGAL))
                    .thenReturn(List.of(documentoGuardado(UUID.randomUUID())));

            List<DocumentoResponse> docs = service.listar(refId, TipoDocumento.PDF_LEGAL);

            assertThat(docs).hasSize(1);
            verify(documentoRepository).findByIdReferenciaAndEntidadReferenciaAndTipoDocumento(
                    refId.toString(), "USUARIO_ACTIVO", TipoDocumento.PDF_LEGAL);
        }

        @Test
        @CP(value = "CP28", scenario = "Listado sin filtro de tipo",
                input = "idReferencia, tipo nulo",
                expected = "Recupera todos los documentos de la entidad")
        void listar_sinTipo_recuperaTodos() {
            UUID refId = UUID.randomUUID();
            when(entidadResolver.resolverEntidad(refId)).thenReturn("ACTIVO");
            when(documentoRepository.findByIdReferenciaAndEntidadReferencia(
                    refId.toString(), "ACTIVO"))
                    .thenReturn(List.of(documentoGuardado(UUID.randomUUID()),
                            documentoGuardado(UUID.randomUUID())));

            List<DocumentoResponse> docs = service.listar(refId, null);

            assertThat(docs).hasSize(2);
        }

        @Test
        @CP(value = "CP28", scenario = "Documentos por referencia incluyen URL firmada",
                input = "entidad + idReferencia",
                expected = "Cada documento se entrega con su Signed URL")
        void obtenerPorReferencia_adjuntaUrlFirmada() throws Exception {
            when(documentoRepository.findByIdReferenciaAndEntidadReferencia("ref-1", "REPORTE"))
                    .thenReturn(List.of(documentoGuardado(UUID.randomUUID())));
            when(storage.signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES), any()))
                    .thenReturn(new URL("https://storage.googleapis.com/firmada"));

            List<DocumentoResponse> docs = service.obtenerPorReferencia("REPORTE", "ref-1");

            assertThat(docs).hasSize(1);
            assertThat(docs.get(0).urlAcceso()).startsWith("https://");
        }
    }

    // ─── Eliminación ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Eliminación de documentos")
    class Eliminacion {

        @Test
        void eliminarDocumento_existente_borraDeGcsYBd() {
            UUID docId = UUID.randomUUID();
            Documento doc = documentoGuardado(docId);
            when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));

            service.eliminarDocumento(docId);

            verify(storage).delete(any(BlobId.class));
            verify(documentoRepository).delete(doc);
        }

        @Test
        void eliminarDocumento_inexistente_lanzaNotFound() {
            UUID docId = UUID.randomUUID();
            when(documentoRepository.findById(docId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.eliminarDocumento(docId))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(storage, never()).delete(any(BlobId.class));
        }

        @Test
        void eliminarDocumento_conUsuarioId_delega() {
            UUID docId = UUID.randomUUID();
            Documento doc = documentoGuardado(docId);
            when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));

            service.eliminarDocumento(docId, 5);

            verify(documentoRepository).delete(doc);
        }
    }

    // ─── Detalle de etapa (Stage) ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Detalle de etapa documental")
    class DetalleEtapa {

        @Test
        void obtenerDetalleEtapa_etapaDistintaDeContrato_devuelveVacio() {
            StageDocumentResponse resp = service.obtenerDetalleEtapa("pago", UUID.randomUUID());

            assertThat(resp.totalCount()).isZero();
            assertThat(resp.documents()).isEmpty();
            verifyNoInteractions(usuarioActivoRepository);
        }

        @Test
        void obtenerDetalleEtapa_contrato_construyeResumenConActivoPrincipal() {
            UUID uaId = UUID.randomUUID();
            Activo activo = Activo.builder()
                    .id(UUID.randomUUID())
                    .nro("402")
                    .tipo(TipoActivo.DEPARTAMENTO)
                    .areaM2(new BigDecimal("85.50"))
                    .precio(new BigDecimal("350000"))
                    .estadoComercial(EstadoComercialActivo.VENDIDO)
                    .build();
            UsuarioActivo ua = UsuarioActivo.builder()
                    .uuidUsuarioActivo(uaId)
                    .activo(activo)
                    .tipoFinanciamiento("Crédito Directo")
                    .fechaAdquisicion(LocalDateTime.of(2026, 3, 15, 10, 0))
                    .build();
            when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.of(ua));

            StageDocumentResponse resp = service.obtenerDetalleEtapa("contrato", uaId);

            assertThat(resp.totalCount()).isEqualTo(1);
            assertThat(resp.documents()).hasSize(1);
            assertThat(resp.documents().get(0).title()).isEqualTo("Dpto. 402");
            assertThat(resp.sectionTitle()).contains("Crédito Directo");
        }

        @Test
        void obtenerDetalleEtapa_contratoUsuarioActivoInexistente_lanzaNotFound() {
            UUID uaId = UUID.randomUUID();
            when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerDetalleEtapa("contrato", uaId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
