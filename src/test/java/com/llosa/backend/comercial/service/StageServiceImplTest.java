package com.llosa.backend.comercial.service;

import com.llosa.backend.annotation.CP;
import com.llosa.backend.comercial.dto.StageDocumentResponse;
import com.llosa.backend.comercial.dto.StageResponse;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.comercial.service.impl.StageServiceImpl;
import com.llosa.backend.documentos.dto.SignedUrlResponse;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.pagos.entity.CartaAprobacion;
import com.llosa.backend.pagos.repository.CartaAprobacionRepository;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
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

/**
 * Pruebas unitarias del servicio de consulta de etapas del proceso comercial
 * (Stepper del Portal del Cliente — CP41/CP44) y de la segregación de accesos
 * por expediente (CP28): un CLIENTE solo puede consultar expedientes de los que
 * es copropietario.
 */
@ExtendWith(MockitoExtension.class)
class StageServiceImplTest {

    @Mock HitoProcesoCompraRepository hitoRepository;
    @Mock UsuarioActivoRepository usuarioActivoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock DocumentoRepository documentoRepository;
    @Mock RequisitoDocumentalRepository requisitoDocumentalRepository;
    @Mock DocumentoService documentoService;
    @Mock CartaAprobacionRepository cartaAprobacionRepository;

    @InjectMocks StageServiceImpl service;

    private static final String FIREBASE_UID = "uid-cliente";

    private Usuario usuario(int id, String tipo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setFirebaseUuid(FIREBASE_UID);
        u.setTipoUsuario(tipo);
        return u;
    }

    private Activo activo() {
        return Activo.builder()
                .id(UUID.randomUUID()).nro("402").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(new BigDecimal("85.50")).precio(new BigDecimal("350000"))
                .estadoComercial(EstadoComercialActivo.VENDIDO).build();
    }

    private UsuarioActivo expediente(UUID id, Usuario... copropietarios) {
        return UsuarioActivo.builder()
                .uuidUsuarioActivo(id)
                .activo(activo())
                .tipoFinanciamiento("Crédito Hipotecario")
                .fechaAdquisicion(LocalDateTime.of(2026, 3, 1, 9, 0))
                .clientes(new ArrayList<>(List.of(copropietarios)))
                .build();
    }

    private HitoProcesoCompra hito(UsuarioActivo ua, EtapaProceso etapa, int orden, EstadoHitoComercial estado) {
        return HitoProcesoCompra.builder()
                .uuidHitoComercial(UUID.randomUUID())
                .usuarioActivo(ua)
                .etapaProceso(etapa)
                .nombreHito("Hito " + orden)
                .orden(orden)
                .estado(estado)
                .build();
    }

    // ─── CP41/CP44: Consulta de etapa (stepper) ──────────────────────────────────

    @Test
    @CP(value = "CP41", scenario = "Consulta de etapa con progreso",
            input = "etapa SEPARACION con 1 de 2 hitos COMPLETADO",
            expected = "progreso 50%, stepIndex y totalSteps coherentes")
    @DisplayName("obtenerStage calcula progreso y filtra hitos por etapa")
    void obtenerStage_calculaProgresoYFiltraPorEtapa() {
        UUID uaId = UUID.randomUUID();
        Usuario cliente = usuario(7, "CLIENTE");
        UsuarioActivo ua = expediente(uaId, cliente);

        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(cliente));
        when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(hitoRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(uaId))
                .thenReturn(List.of(
                        hito(ua, EtapaProceso.SEPARACION, 1, EstadoHitoComercial.COMPLETADO),
                        hito(ua, EtapaProceso.SEPARACION, 2, EstadoHitoComercial.PENDIENTE),
                        hito(ua, EtapaProceso.CONTRATO, 3, EstadoHitoComercial.PENDIENTE)));

        StageResponse resp = service.obtenerStage(FIREBASE_UID, uaId, EtapaProceso.SEPARACION);

        assertThat(resp.stepper()).hasSize(2); // solo los de SEPARACION
        assertThat(resp.stage().progressPercentage()).isEqualTo(50.0);
        assertThat(resp.stage().stepIndex()).isEqualTo(1); // SEPARACION es la primera etapa
        assertThat(resp.stage().totalSteps()).isEqualTo(EtapaProceso.values().length);
        assertThat(resp.stageDetails()).isNull(); // solo CONTRATO trae detalles
    }

    @Test
    @CP(value = "CP41", scenario = "Detalle de contrato con carta de aprobación",
            input = "etapa CONTRATO con CartaAprobacion existente",
            expected = "stageDetails con datos del activo y de la carta bancaria")
    void obtenerStage_contratoConCarta_incluyeDetalleBancario() {
        UUID uaId = UUID.randomUUID();
        Usuario cliente = usuario(7, "CLIENTE");
        UsuarioActivo ua = expediente(uaId, cliente);

        CartaAprobacion carta = new CartaAprobacion();
        carta.setBanco("BCP");
        carta.setMontoAprobado(new BigDecimal("300000"));
        carta.setFechaEmision(LocalDate.of(2026, 2, 1));
        carta.setFechaVencimiento(LocalDate.of(2026, 8, 1));
        carta.setFechaDesembolsoProyectada(LocalDate.of(2026, 4, 1));
        carta.setComentarios("Aprobado sujeto a tasación");

        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(cliente));
        when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(hitoRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(uaId))
                .thenReturn(List.of(hito(ua, EtapaProceso.CONTRATO, 1, EstadoHitoComercial.COMPLETADO)));
        when(cartaAprobacionRepository.findByUsuarioActivo_UuidUsuarioActivo(uaId))
                .thenReturn(Optional.of(carta));

        StageResponse resp = service.obtenerStage(FIREBASE_UID, uaId, EtapaProceso.CONTRATO);

        assertThat(resp.stageDetails()).isNotNull();
        assertThat(resp.stageDetails().banco()).isEqualTo("BCP");
        assertThat(resp.stageDetails().montoAprobado()).contains("300,000");
        assertThat(resp.stage().stepIndex()).isEqualTo(2); // CONTRATO es la segunda etapa
    }

    @Test
    void obtenerStage_contratoSinCarta_detalleSinDatosBancarios() {
        UUID uaId = UUID.randomUUID();
        Usuario cliente = usuario(7, "CLIENTE");
        UsuarioActivo ua = expediente(uaId, cliente);

        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(cliente));
        when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(hitoRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(uaId))
                .thenReturn(List.of());
        when(cartaAprobacionRepository.findByUsuarioActivo_UuidUsuarioActivo(uaId))
                .thenReturn(Optional.empty());

        StageResponse resp = service.obtenerStage(FIREBASE_UID, uaId, EtapaProceso.CONTRATO);

        assertThat(resp.stage().progressPercentage()).isZero(); // sin hitos
        assertThat(resp.stageDetails()).isNotNull();
        assertThat(resp.stageDetails().banco()).isNull();
        assertThat(resp.stageDetails().totalPrice()).contains("350,000");
    }

    // ─── CP28: Segregación de accesos ─────────────────────────────────────────────

    @Test
    @CP(value = "CP28", scenario = "Cliente intenta ver expediente ajeno",
            input = "CLIENTE que no es copropietario del expediente",
            expected = "AccessDeniedException; no expone datos")
    @DisplayName("obtenerStage niega acceso a un CLIENTE sobre expediente ajeno")
    void obtenerStage_clienteNoPropietario_lanzaAccessDenied() {
        UUID uaId = UUID.randomUUID();
        Usuario intruso = usuario(99, "CLIENTE");
        Usuario propietario = usuario(7, "CLIENTE");
        propietario.setId(7);
        UsuarioActivo ua = expediente(uaId, propietario); // intruso (99) no está

        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(intruso));
        when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.of(ua));

        assertThatThrownBy(() -> service.obtenerStage(FIREBASE_UID, uaId, EtapaProceso.SEPARACION))
                .isInstanceOf(AccessDeniedException.class);
        verify(hitoRepository, never()).findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(any());
    }

    @Test
    @CP(value = "CP28", scenario = "Asesor accede a cualquier expediente",
            input = "usuario tipo ASESOR no copropietario",
            expected = "Acceso permitido (la restricción aplica solo a CLIENTE)")
    void obtenerStage_asesorNoPropietario_permiteAcceso() {
        UUID uaId = UUID.randomUUID();
        Usuario asesor = usuario(50, "ASESOR");
        UsuarioActivo ua = expediente(uaId, usuario(7, "CLIENTE"));

        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(asesor));
        when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(hitoRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(uaId))
                .thenReturn(List.of());

        StageResponse resp = service.obtenerStage(FIREBASE_UID, uaId, EtapaProceso.SEPARACION);

        assertThat(resp).isNotNull();
    }

    @Test
    void obtenerStage_expedienteInexistente_lanzaRecursoNoEncontrado() {
        UUID uaId = UUID.randomUUID();
        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(usuario(7, "CLIENTE")));
        when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerStage(FIREBASE_UID, uaId, EtapaProceso.SEPARACION))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerStage_usuarioInexistente_lanzaNotFound() {
        UUID uaId = UUID.randomUUID();
        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerStage(FIREBASE_UID, uaId, EtapaProceso.SEPARACION))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── Documentos de la etapa ───────────────────────────────────────────────────

    @Test
    @CP(value = "CP25", scenario = "Documentos de etapa con archivo descargable",
            input = "requisito COMPLETADA con documento físico",
            expected = "Item con hasDownload=true y Signed URL")
    void obtenerDocumentosStage_requisitoConArchivo_generaUrlFirmada() {
        UUID uaId = UUID.randomUUID();
        Usuario cliente = usuario(7, "CLIENTE");
        UsuarioActivo ua = expediente(uaId, cliente);
        HitoProcesoCompra hito = hito(ua, EtapaProceso.CONTRATO, 1, EstadoHitoComercial.EN_PROGRESO);

        UUID reqId = UUID.randomUUID();
        RequisitoDocumental req = RequisitoDocumental.builder()
                .id(reqId).hitoComercial(hito).titulo("Minuta")
                .estado("COMPLETADA").fechaEmision(LocalDate.of(2026, 3, 5)).build();
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.builder().id(docId).build();

        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(cliente));
        when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(hitoRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uaId, EtapaProceso.CONTRATO))
                .thenReturn(Optional.of(hito));
        when(requisitoDocumentalRepository
                .findByHitoComercial_UuidHitoComercialOrderByFechaEmisionDesc(hito.getUuidHitoComercial()))
                .thenReturn(List.of(req));
        when(documentoRepository.findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                "REQUISITO", reqId.toString())).thenReturn(Optional.of(doc));
        when(documentoService.generarSignedUrl(docId))
                .thenReturn(new SignedUrlResponse("https://signed-url", Instant.now()));

        StageDocumentResponse resp = service.obtenerDocumentosStage(FIREBASE_UID, uaId, EtapaProceso.CONTRATO);

        assertThat(resp.documents()).hasSize(1);
        assertThat(resp.documents().get(0).hasDownload()).isTrue();
        assertThat(resp.documents().get(0).downloadUrl()).isEqualTo("https://signed-url");
        assertThat(resp.documents().get(0).status()).isEqualTo("completada");
    }

    @Test
    void obtenerDocumentosStage_requisitoSinArchivo_noGeneraUrl() {
        UUID uaId = UUID.randomUUID();
        Usuario cliente = usuario(7, "CLIENTE");
        UsuarioActivo ua = expediente(uaId, cliente);
        HitoProcesoCompra hito = hito(ua, EtapaProceso.CONTRATO, 1, EstadoHitoComercial.PENDIENTE);

        UUID reqId = UUID.randomUUID();
        RequisitoDocumental req = RequisitoDocumental.builder()
                .id(reqId).hitoComercial(hito).titulo("Escritura").estado("PENDIENTE").build();

        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(cliente));
        when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(hitoRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uaId, EtapaProceso.CONTRATO))
                .thenReturn(Optional.of(hito));
        when(requisitoDocumentalRepository
                .findByHitoComercial_UuidHitoComercialOrderByFechaEmisionDesc(hito.getUuidHitoComercial()))
                .thenReturn(List.of(req));
        when(documentoRepository.findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                "REQUISITO", reqId.toString())).thenReturn(Optional.empty());

        StageDocumentResponse resp = service.obtenerDocumentosStage(FIREBASE_UID, uaId, EtapaProceso.CONTRATO);

        assertThat(resp.documents().get(0).hasDownload()).isFalse();
        assertThat(resp.documents().get(0).downloadUrl()).isNull();
        verify(documentoService, never()).generarSignedUrl(any());
    }

    @Test
    void obtenerDocumentosStage_sinHitoComercial_lanzaNotFound() {
        UUID uaId = UUID.randomUUID();
        Usuario cliente = usuario(7, "CLIENTE");
        UsuarioActivo ua = expediente(uaId, cliente);

        when(usuarioRepository.findByFirebaseUuid(FIREBASE_UID)).thenReturn(Optional.of(cliente));
        when(usuarioActivoRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(hitoRepository.findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uaId, EtapaProceso.CONTRATO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerDocumentosStage(FIREBASE_UID, uaId, EtapaProceso.CONTRATO))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
