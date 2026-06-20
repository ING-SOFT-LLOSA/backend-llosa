package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.dto.StageActivosResponse;
import com.llosa.backend.comercial.dto.StageDocumentsResponse;
import com.llosa.backend.comercial.dto.StageTrackerResponse;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.comercial.service.impl.StageServiceImpl;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StageServiceImplTest {

    @Mock EtapaExpedienteRepository etapaExpedienteRepository;
    @Mock HitoProcesoCompraRepository hitoRepository;
    @Mock RequisitoDocumentalRepository requisitoDocumentalRepository;
    @Mock UsuarioActivoRepository usuarioActivoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock DocumentoRepository documentoRepository;
    @Mock DocumentoService documentoService;

    @InjectMocks StageServiceImpl stageService;

    private final String firebaseUid = "firebase-uid";
    private final UUID uuidUa = UUID.randomUUID();
    private final UUID uuidEtapa = UUID.randomUUID();

    private Usuario buildUsuario(String tipo) {
        var u = new Usuario();
        u.setId(1);
        u.setFirebaseUuid(firebaseUid);
        u.setTipoUsuario(tipo);
        u.setNombre("Test");
        u.setEmail("test@test.com");
        return u;
    }

    private UsuarioActivo buildUa(List<Usuario> clientes) {
        return UsuarioActivo.builder()
                .uuidUsuarioActivo(uuidUa)
                .clientes(clientes != null ? clientes : new ArrayList<>())
                .activos(new ArrayList<>())
                .fechaAdquisicion(LocalDateTime.now())
                .tipoFinanciamiento("Credito Directo")
                .build();
    }

    private EtapaExpediente buildEtapa(EtapaProceso proceso) {
        return EtapaExpediente.builder()
                .uuidEtapaExpediente(uuidEtapa)
                .etapaProceso(proceso)
                .build();
    }

    // ─── obtenerStage ──────────────────────────────────────────────────

    @Test
    void obtenerStage_exitoso() {
        var usuario = buildUsuario("ADMIN");
        var ua = buildUa(List.of(usuario));
        var etapa = buildEtapa(EtapaProceso.SEPARACION);
        var hitos = List.of(
                HitoProcesoCompra.builder().nombreHito("Hito 1").estado(EstadoHitoComercial.COMPLETADO).orden(1).build(),
                HitoProcesoCompra.builder().nombreHito("Hito 2").estado(EstadoHitoComercial.PENDIENTE).orden(2).build()
        );

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.SEPARACION))
                .thenReturn(Optional.of(etapa));
        when(hitoRepository.findByEtapaExpediente_UuidEtapaExpedienteOrderByOrdenAsc(uuidEtapa))
                .thenReturn(hitos);

        var result = stageService.obtenerStage(firebaseUid, uuidUa, EtapaProceso.SEPARACION);

        assertThat(result.stage().title()).isEqualTo("Separacion");
        assertThat(result.stepper()).hasSize(2);
        assertThat(result.stage().progressPercentage()).isEqualTo(50.0);
    }

    @Test
    void obtenerStage_etapaNoExiste_lanzaEntityNotFound() {
        var usuario = buildUsuario("ADMIN");
        var ua = buildUa(List.of(usuario));

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.SEPARACION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> stageService.obtenerStage(firebaseUid, uuidUa, EtapaProceso.SEPARACION))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void obtenerStage_usuarioNoExiste_lanzaEntityNotFound() {
        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stageService.obtenerStage(firebaseUid, uuidUa, EtapaProceso.SEPARACION))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void obtenerStage_clienteSinAcceso_lanzaAccessDenied() {
        var usuario = buildUsuario("CLIENTE");
        var otroCliente = new Usuario();
        otroCliente.setId(2);
        var ua = buildUa(List.of(otroCliente));

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));

        assertThatThrownBy(() -> stageService.obtenerStage(firebaseUid, uuidUa, EtapaProceso.SEPARACION))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerStage_clienteConAcceso_ok() {
        var usuario = buildUsuario("CLIENTE");
        var ua = buildUa(List.of(usuario));
        var etapa = buildEtapa(EtapaProceso.SEPARACION);

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.SEPARACION))
                .thenReturn(Optional.of(etapa));
        when(hitoRepository.findByEtapaExpediente_UuidEtapaExpedienteOrderByOrdenAsc(uuidEtapa))
                .thenReturn(List.of());

        var result = stageService.obtenerStage(firebaseUid, uuidUa, EtapaProceso.SEPARACION);
        assertThat(result).isNotNull();
    }

    @Test
    void obtenerStage_contrato_incluyeStageDetails() {
        var usuario = buildUsuario("ADMIN");
        var activo = Activo.builder().id(UUID.randomUUID()).nro("A-101")
                .tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(new BigDecimal("80")).precio(new BigDecimal("300000"))
                .build();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuidUa)
                .clientes(List.of(usuario))
                .activos(List.of(activo))
                .fechaAdquisicion(LocalDateTime.now())
                .build();
        var etapa = buildEtapa(EtapaProceso.CONTRATO);

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.CONTRATO))
                .thenReturn(Optional.of(etapa));
        when(hitoRepository.findByEtapaExpediente_UuidEtapaExpedienteOrderByOrdenAsc(uuidEtapa))
                .thenReturn(List.of());

        var result = stageService.obtenerStage(firebaseUid, uuidUa, EtapaProceso.CONTRATO);

        assertThat(result.stageDetails()).isNotNull();
        assertThat(result.stageDetails().area()).contains("80");
        assertThat(result.stageDetails().totalPrice()).contains("300,000");
    }

    @Test
    void obtenerStage_contrato_sinActivos_stageDetailsNull() {
        var usuario = buildUsuario("ADMIN");
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuidUa)
                .clientes(List.of(usuario))
                .activos(new ArrayList<>())
                .build();
        var etapa = buildEtapa(EtapaProceso.CONTRATO);

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.CONTRATO))
                .thenReturn(Optional.of(etapa));
        when(hitoRepository.findByEtapaExpediente_UuidEtapaExpedienteOrderByOrdenAsc(uuidEtapa))
                .thenReturn(List.of());

        var result = stageService.obtenerStage(firebaseUid, uuidUa, EtapaProceso.CONTRATO);
        assertThat(result.stageDetails().area()).isNull();
        assertThat(result.stageDetails().totalPrice()).isNull();
    }

    @Test
    void obtenerStage_sinHitos_progresoCero() {
        var usuario = buildUsuario("ADMIN");
        var ua = buildUa(List.of(usuario));
        var etapa = buildEtapa(EtapaProceso.SEPARACION);

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.SEPARACION))
                .thenReturn(Optional.of(etapa));
        when(hitoRepository.findByEtapaExpediente_UuidEtapaExpedienteOrderByOrdenAsc(uuidEtapa))
                .thenReturn(List.of());

        var result = stageService.obtenerStage(firebaseUid, uuidUa, EtapaProceso.SEPARACION);
        assertThat(result.stage().progressPercentage()).isZero();
    }

    // ─── obtenerDocumentosStage ───────────────────────────────────────

    @Test
    void obtenerDocumentosStage_exitoso() {
        var usuario = buildUsuario("ADMIN");
        var ua = buildUa(List.of(usuario));
        var etapa = buildEtapa(EtapaProceso.SEPARACION);
        var requisito = RequisitoDocumental.builder()
                .id(UUID.randomUUID())
                .titulo("Documento 1")
                .descripcion("Desc")
                .estado(com.llosa.backend.comercial.enums.EtapaRequisitoDocumental.PENDIENTE)
                .icono("description")
                .build();
        var documento = Documento.builder().id(UUID.randomUUID()).idReferencia(requisito.getId().toString()).build();

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.SEPARACION))
                .thenReturn(Optional.of(etapa));
        when(requisitoDocumentalRepository.findByEtapaExpediente_UuidEtapaExpedienteOrderByFechaEmisionDesc(uuidEtapa))
                .thenReturn(List.of(requisito));
        when(documentoRepository.findByEntidadReferenciaAndIdReferenciaInOrderByCreatedAtDesc(
                eq("REQUISITO"), anyList())).thenReturn(List.of(documento));
        when(documentoService.generarSignedUrl(documento.getId()))
                .thenReturn(new SignedUrlResponse("https://url.com/doc", Instant.now()));

        var result = stageService.obtenerDocumentosStage(firebaseUid, uuidUa, EtapaProceso.SEPARACION);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.documents().get(0).hasDownload()).isTrue();
    }

    @Test
    void obtenerDocumentosStage_etapaNoExiste_lanzaEntityNotFound() {
        var usuario = buildUsuario("ADMIN");
        var ua = buildUa(List.of(usuario));

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.SEPARACION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> stageService.obtenerDocumentosStage(firebaseUid, uuidUa, EtapaProceso.SEPARACION))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void obtenerDocumentosStage_sinRequisitos_retornaVacio() {
        var usuario = buildUsuario("ADMIN");
        var ua = buildUa(List.of(usuario));
        var etapa = buildEtapa(EtapaProceso.SEPARACION);

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));
        when(etapaExpedienteRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUa, EtapaProceso.SEPARACION))
                .thenReturn(Optional.of(etapa));
        when(requisitoDocumentalRepository.findByEtapaExpediente_UuidEtapaExpedienteOrderByFechaEmisionDesc(uuidEtapa))
                .thenReturn(List.of());

        var result = stageService.obtenerDocumentosStage(firebaseUid, uuidUa, EtapaProceso.SEPARACION);
        assertThat(result.totalCount()).isZero();
    }

    // ─── obtenerActivosEtapa ──────────────────────────────────────────

    @Test
    void obtenerActivosEtapa_noContrato_retornaVacio() {
        var result = stageService.obtenerActivosEtapa(firebaseUid, uuidUa, EtapaProceso.SEPARACION);
        assertThat(result.totalActivos()).isZero();
        assertThat(result.activos()).isEmpty();
    }

    @Test
    void obtenerActivosEtapa_contrato_exitoso() {
        var usuario = buildUsuario("ADMIN");
        var activo = Activo.builder().id(UUID.randomUUID()).nro("A-101")
                .tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(new BigDecimal("80")).precio(new BigDecimal("300000"))
                .descripcion("Descripcion").tieneRecorridoVirtual(true)
                .build();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuidUa)
                .clientes(List.of(usuario))
                .activos(List.of(activo))
                .fechaAdquisicion(LocalDateTime.now())
                .tipoFinanciamiento("Credito Directo")
                .build();

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));

        var result = stageService.obtenerActivosEtapa(firebaseUid, uuidUa, EtapaProceso.CONTRATO);

        assertThat(result.totalActivos()).isEqualTo(1);
        assertThat(result.activos().get(0).nro()).isEqualTo("A-101");
        assertThat(result.resumen()).contains("Credito Directo");
    }

    @Test
    void obtenerActivosEtapa_sinFechaAdquisicion_resumenPendiente() {
        var usuario = buildUsuario("ADMIN");
        var activo = Activo.builder().id(UUID.randomUUID()).nro("A-101")
                .tipo(TipoActivo.DEPARTAMENTO).areaM2(new BigDecimal("80"))
                .precio(new BigDecimal("300000")).build();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuidUa)
                .clientes(List.of(usuario))
                .activos(List.of(activo))
                .fechaAdquisicion(null)
                .tipoFinanciamiento(null)
                .build();

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));

        var result = stageService.obtenerActivosEtapa(firebaseUid, uuidUa, EtapaProceso.CONTRATO);
        assertThat(result.resumen()).contains("Pendiente").contains("-");
    }

    @Test
    void obtenerActivosEtapa_activosNull_retornaVacio() {
        var usuario = buildUsuario("ADMIN");
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(uuidUa)
                .clientes(List.of(usuario))
                .activos(null)
                .build();

        when(usuarioRepository.findByFirebaseUuid(firebaseUid)).thenReturn(Optional.of(usuario));
        when(usuarioActivoRepository.findById(uuidUa)).thenReturn(Optional.of(ua));

        var result = stageService.obtenerActivosEtapa(firebaseUid, uuidUa, EtapaProceso.CONTRATO);
        assertThat(result.totalActivos()).isZero();
    }
}
