package com.llosa.backend.documentos.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.llosa.backend.comercial.dto.StageDocumentsResponse;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.dto.SubirDocumentoRequest;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.entity.TipoDocumentoConfig;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.repository.TipoDocumentoConfigRepository;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock DocumentoRepository documentoRepository;
    @Mock TipoDocumentoConfigRepository tipoDocumentoConfigRepository;
    @Mock EntidadResolverService entidadResolver;
    @Mock UsuarioActivoRepository usuarioActivoRepository;
    @Mock Storage storage;
    @InjectMocks DocumentoService documentoService;

    private final UUID uuid = UUID.randomUUID();
    private final String bucketName = "test-bucket";

    private Piso buildPiso() {
        var proyecto = Proyecto.builder().id(UUID.randomUUID()).nombre("Test Proyecto").build();
        var torre = Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();
        return Piso.builder().id(1L).nroPiso(1).torre(torre).build();
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentoService, "gcsBucketName", bucketName);
    }

    @Test
    void obtenerDetalleEtapa_noContrato_retornaVacio() {
        var result = documentoService.obtenerDetalleEtapa(EtapaProceso.SEPARACION, uuid);
        assertThat(result.totalCount()).isZero();
        assertThat(result.documents()).isEmpty();
        assertThat(result.title()).isNull();
    }

    @Test
    void obtenerDetalleEtapa_contrato_sinActivos_retornaVacio() {
        var ua = UsuarioActivo.builder().uuidUsuarioActivo(uuid).activos(null).build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));

        var result = documentoService.obtenerDetalleEtapa(EtapaProceso.CONTRATO, uuid);
        assertThat(result.totalCount()).isZero();
    }

    @Test
    void obtenerDetalleEtapa_contrato_conActivos_mapeaItems() {
        var activo = Activo.builder()
                .id(uuid).nro("A-101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(new BigDecimal("80.50")).precio(new BigDecimal("350000"))
                .estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(buildPiso())
                .build();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuid)
                .activos(List.of(activo))
                .fechaAdquisicion(LocalDateTime.now())
                .tipoFinanciamiento("Credito Directo")
                .build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));

        var result = documentoService.obtenerDetalleEtapa(EtapaProceso.CONTRATO, uuid);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.documents().get(0).title()).contains("Dpto.");
        assertThat(result.title()).contains("Contrato firmado");
    }

    @Test
    void obtenerDetalleEtapa_contrato_uaNoExiste_lanzaEntityNotFound() {
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> documentoService.obtenerDetalleEtapa(EtapaProceso.CONTRATO, uuid))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void subirDocumento_exitoso() throws Exception {
        var file = mock(MultipartFile.class);
        var request = new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL);
        var config = new TipoDocumentoConfig();
        config.setMimePermitidos("application/pdf");
        config.setMaxSizeBytes(10_485_760L);

        when(entidadResolver.resolverEntidad(uuid)).thenReturn("PROYECTO");
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("doc.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L);
        doReturn(new byte[]{1, 2, 3}).when(file).getBytes();
        when(tipoDocumentoConfigRepository.findById(TipoDocumento.PDF_LEGAL)).thenReturn(Optional.of(config));
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(mock(com.google.cloud.storage.Blob.class));
        when(documentoRepository.save(any())).thenAnswer(inv -> {
            Documento d = inv.getArgument(0);
            var idField = Documento.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(d, UUID.randomUUID());
            return d;
        });

        var result = documentoService.subirDocumento(uuid, file, request, 1);

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        verify(documentoRepository).save(any());
    }

    @Test
    void subirDocumento_errorGcs_lanzaBusinessException() throws Exception {
        var file = mock(MultipartFile.class);
        var request = new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL);
        var config = new TipoDocumentoConfig();
        config.setMimePermitidos("application/pdf");
        config.setMaxSizeBytes(10_485_760L);

        when(entidadResolver.resolverEntidad(uuid)).thenReturn("PROYECTO");
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("doc.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L);
        when(tipoDocumentoConfigRepository.findById(TipoDocumento.PDF_LEGAL)).thenReturn(Optional.of(config));
        
        when(file.getBytes()).thenThrow(new IOException("GCS Error"));

        assertThatThrownBy(() -> documentoService.subirDocumento(uuid, file, request, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Error al subir el archivo a GCS");
    }

    @Test
    void subirDocumento_archivoVacio_lanzaBusinessException() {
        var file = mock(MultipartFile.class);
        var request = new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL);

        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> documentoService.subirDocumento(uuid, file, request, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void subirDocumento_tipoNoConfigurado_lanzaBusinessException() {
        var file = mock(MultipartFile.class);
        var request = new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL);

        when(file.isEmpty()).thenReturn(false);
        when(tipoDocumentoConfigRepository.findById(TipoDocumento.PDF_LEGAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoService.subirDocumento(uuid, file, request, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no configurado");
    }

    @Test
    void subirDocumento_mimeNoPermitido_lanzaBusinessException() {
        var file = mock(MultipartFile.class);
        var request = new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL);
        var config = new TipoDocumentoConfig();
        config.setMimePermitidos("image/png,image/jpeg");
        config.setMaxSizeBytes(10_485_760L);

        // 👇 SE ELIMINÓ EL STUB DE entidadResolver PORQUE ES INNECESARIO
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("foto.png");
        when(file.getContentType()).thenReturn("application/pdf");
        when(tipoDocumentoConfigRepository.findById(TipoDocumento.PDF_LEGAL)).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> documentoService.subirDocumento(uuid, file, request, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no permitido");
    }

    @Test
    void subirDocumento_excedeTamanio_lanzaBusinessException() {
        var file = mock(MultipartFile.class);
        var request = new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL);
        var config = new TipoDocumentoConfig();
        config.setMimePermitidos("application/pdf");
        config.setMaxSizeBytes(1024L);

        // 👇 SE ELIMINÓ EL STUB DE entidadResolver PORQUE ES INNECESARIO
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("documento.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(2048L);
        when(tipoDocumentoConfigRepository.findById(TipoDocumento.PDF_LEGAL)).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> documentoService.subirDocumento(uuid, file, request, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("El archivo supera");
    }

    @Test
    void listar_sinTipoDocumento_retornaTodos() {
        when(entidadResolver.resolverEntidad(uuid)).thenReturn("PROYECTO");
        when(documentoRepository.findByIdReferenciaAndEntidadReferencia(uuid.toString(), "PROYECTO"))
                .thenReturn(List.of());

        var result = documentoService.listar(uuid, null);
        assertThat(result).isEmpty();
    }

    @Test
    void listar_conTipoDocumento_filtra() {
        when(entidadResolver.resolverEntidad(uuid)).thenReturn("PROYECTO");
        when(documentoRepository.findByIdReferenciaAndEntidadReferenciaAndTipoDocumento(
                uuid.toString(), "PROYECTO", TipoDocumento.PDF_LEGAL))
                .thenReturn(List.of());

        var result = documentoService.listar(uuid, TipoDocumento.PDF_LEGAL);
        assertThat(result).isEmpty();
    }

    @Test
    void generarSignedUrl_exitoso() {
        var docId = UUID.randomUUID();
        var doc = Documento.builder().id(docId).rutaGcs("ruta/test.pdf").build();
        var url = mock(URL.class);
        when(url.toString()).thenReturn("https://signed.url/test");
        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any(Storage.SignUrlOption.class)))
                .thenReturn(url);

        var result = documentoService.generarSignedUrl(docId);
        assertThat(result.url()).isEqualTo("https://signed.url/test");
    }

    @Test
    void generarSignedUrl_noExiste_lanzaEntityNotFound() {
        var docId = UUID.randomUUID();
        when(documentoRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoService.generarSignedUrl(docId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void generarSignedUrl_conUsuarioId_delega() {
        var docId = UUID.randomUUID();
        var doc = Documento.builder().id(docId).rutaGcs("ruta/test.pdf").build();
        var url = mock(URL.class);
        when(url.toString()).thenReturn("https://signed.url/test");
        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any(Storage.SignUrlOption.class)))
                .thenReturn(url);

        var result = documentoService.generarSignedUrl(docId, 1);
        assertThat(result.url()).isEqualTo("https://signed.url/test");
    }

    @Test
    void eliminarDocumento_exitoso() {
        var docId = UUID.randomUUID();
        var doc = Documento.builder().id(docId).rutaGcs("ruta/test.pdf").build();
        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));

        documentoService.eliminarDocumento(docId);

        verify(storage).delete(BlobId.of(bucketName, "ruta/test.pdf"));
        verify(documentoRepository).delete(doc);
    }

    @Test
    void eliminarDocumento_noExiste_lanzaEntityNotFound() {
        var docId = UUID.randomUUID();
        when(documentoRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoService.eliminarDocumento(docId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void eliminarDocumento_conUsuarioId_delega() {
        var docId = UUID.randomUUID();
        var doc = Documento.builder().id(docId).rutaGcs("ruta/test.pdf").build();
        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));

        documentoService.eliminarDocumento(docId, 1);

        verify(storage).delete(BlobId.of(bucketName, "ruta/test.pdf"));
        verify(documentoRepository).delete(doc);
    }

    @Test
    void obtenerDetalleEtapa_contrato_areaTotalSuma() {
        var a1 = Activo.builder().id(UUID.randomUUID()).nro("A-1")
                .tipo(TipoActivo.DEPARTAMENTO).areaM2(new BigDecimal("50"))
                .precio(new BigDecimal("100000")).estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(buildPiso()).build();
        var a2 = Activo.builder().id(UUID.randomUUID()).nro("C-1")
                .tipo(TipoActivo.COCHERA).areaM2(new BigDecimal("12.5"))
                .precio(new BigDecimal("20000")).estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(buildPiso()).build();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuid)
                .activos(List.of(a1, a2))
                .fechaAdquisicion(LocalDateTime.now())
                .tipoFinanciamiento("Credito Directo")
                .build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));

        var result = documentoService.obtenerDetalleEtapa(EtapaProceso.CONTRATO, uuid);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.title()).contains("62.50 m²");
    }

    @Test
    void obtenerDetalleEtapa_sinFechaAdquisicion_resumenPendiente() {
        var activo = Activo.builder().id(uuid).nro("A-101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(new BigDecimal("80.50")).precio(new BigDecimal("350000"))
                .estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(buildPiso())
                .build();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuid).activos(List.of(activo))
                .fechaAdquisicion(null).tipoFinanciamiento("Credito Directo").build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));

        var result = documentoService.obtenerDetalleEtapa(EtapaProceso.CONTRATO, uuid);
        assertThat(result.title()).contains("Pendiente");
    }

    @Test
    void obtenerPorReferencia_retornaDocumentos() {
        var doc = Documento.builder().id(UUID.randomUUID()).rutaGcs("ruta/doc.pdf").build();
        when(documentoRepository.findByIdReferenciaAndEntidadReferencia("ref1", "PROYECTO"))
                .thenReturn(List.of(doc));
        var url = mock(URL.class);
        when(url.toString()).thenReturn("https://signed.url/doc");
        when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any(Storage.SignUrlOption.class)))
                .thenReturn(url);

        var result = documentoService.obtenerPorReferencia("PROYECTO", "ref1");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).urlAcceso()).isEqualTo("https://signed.url/doc");
    }

    @Test
    void obtenerDetalleEtapa_contrato_tipoCocheraMapeo() {
        var activo = Activo.builder().id(uuid).nro("C-01").tipo(TipoActivo.COCHERA)
                .areaM2(new BigDecimal("12.5")).precio(new BigDecimal("20000"))
                .estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(buildPiso()).build();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuid).activos(List.of(activo))
                .fechaAdquisicion(LocalDateTime.now()).tipoFinanciamiento("Credito Directo").build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));

        var result = documentoService.obtenerDetalleEtapa(EtapaProceso.CONTRATO, uuid);
        assertThat(result.documents().get(0).title()).contains("Cochera");
    }

    @Test
    void obtenerDetalleEtapa_contrato_tipoDepositoMapeo() {
        var activo = Activo.builder().id(uuid).nro("D-01").tipo(TipoActivo.DEPOSITO)
                .areaM2(new BigDecimal("5.0")).precio(new BigDecimal("8000"))
                .estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(buildPiso()).build();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuid).activos(List.of(activo))
                .fechaAdquisicion(LocalDateTime.now()).tipoFinanciamiento("Contado").build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));

        var result = documentoService.obtenerDetalleEtapa(EtapaProceso.CONTRATO, uuid);

        // DEPOSITO mapea título = nro tal cual, icono="file".
        assertThat(result.documents().get(0).title()).isEqualTo("D-01");
        assertThat(result.documents().get(0).icon()).isEqualTo("file");
    }

    @Test
    void obtenerDetalleEtapa_contrato_tipoNulo_mapeaComoDeposito() {
        var activo = Activo.builder().id(uuid).nro("X-99").tipo(null)
                .areaM2(new BigDecimal("3.0")).precio(new BigDecimal("1000"))
                .estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(buildPiso()).build();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuid).activos(List.of(activo))
                .fechaAdquisicion(LocalDateTime.now()).tipoFinanciamiento("Contado").build();
        when(usuarioActivoRepository.findById(uuid)).thenReturn(Optional.of(ua));

        var result = documentoService.obtenerDetalleEtapa(EtapaProceso.CONTRATO, uuid);

        // tipo nulo cae en la rama por defecto (DEPOSITO): icono="file", título = nro.
        assertThat(result.documents().get(0).icon()).isEqualTo("file");
        assertThat(result.documents().get(0).title()).isEqualTo("X-99");
    }

    // ─── crearReferenciaDocumento ────────────────────────────────────────────

    @Test
    void crearReferenciaDocumento_persisteDocumentoConAccesoRestringido() {
        ArgumentCaptor<Documento> captor = ArgumentCaptor.forClass(Documento.class);
        when(documentoRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        DocumentoResponse resp = documentoService.crearReferenciaDocumento(
                "gcs://bucket/ruta/archivo.pdf",
                "archivo.pdf",
                "application/pdf",
                "ref-123",
                "RequisitoDocumental",
                TipoDocumento.PDF_LEGAL,
                7);

        assertThat(resp).isNotNull();
        Documento guardado = captor.getValue();
        assertThat(guardado.getRutaGcs()).isEqualTo("gcs://bucket/ruta/archivo.pdf");
        assertThat(guardado.getNombreOriginal()).isEqualTo("archivo.pdf");
        assertThat(guardado.getIdReferencia()).isEqualTo("ref-123");
        assertThat(guardado.getEntidadReferencia()).isEqualTo("RequisitoDocumental");
        assertThat(guardado.getTipoMime()).isEqualTo("application/pdf");
        assertThat(guardado.isAccesoRestringido()).isTrue();
        assertThat(guardado.getSubidoPor()).isEqualTo(7);
    }
}
