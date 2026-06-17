package com.llosa.backend.pagos.service.impl;

import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.pagos.dto.CreditoHipotecarioResponse;
import com.llosa.backend.pagos.entity.CartaAprobacion;
import com.llosa.backend.pagos.repository.CartaAprobacionRepository;
import com.llosa.backend.pagos.service.CreditoHipotecarioService;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CreditoHipotecarioServiceImpl implements CreditoHipotecarioService {

    private static final String ENTIDAD_REFERENCIA_REQUISITO = "REQUISITO";

    private final UsuarioActivoRepository usuarioActivoRepository;
    private final CartaAprobacionRepository cartaAprobacionRepository;
    private final EtapaExpedienteRepository etapaExpedienteRepository;
    private final HitoProcesoCompraRepository hitoRepository;
    private final RequisitoDocumentalRepository requisitoDocumentalRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoService documentoService;
    private final UsuarioRepository usuarioRepository;

    @Override
    public CreditoHipotecarioResponse obtenerResumen(UUID uuidUsuarioActivo, String firebaseUid) {
        UsuarioActivo ua = validarAcceso(firebaseUid, uuidUsuarioActivo);

        if (ua.getTipoFinanciamiento() == null
                || !ua.getTipoFinanciamiento().toLowerCase(Locale.ROOT).contains("hipotecario")) {
            throw new BusinessException("El expediente no es de tipo crédito hipotecario");
        }

        CartaAprobacion carta = cartaAprobacionRepository
                .findByUsuarioActivo_UuidUsuarioActivo(uuidUsuarioActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay carta de aprobación para el expediente: " + uuidUsuarioActivo));

        List<EtapaExpediente> etapas = etapaExpedienteRepository
                .findByUsuarioActivo_UuidUsuarioActivo(uuidUsuarioActivo);

        List<HitoProcesoCompra> hitos = etapas.stream()
                .flatMap(e -> e.getHitosComerciales().stream())
                .toList();

        Map<String, HitoProcesoCompra> hitosPorNombre = hitos.stream()
                .collect(Collectors.toMap(
                        h -> normalizar(h.getNombreHito()),
                        h -> h,
                        (h1, h2) -> h1
                ));

        List<RequisitoDocumental> requisitos = etapas.stream()
                .flatMap(e -> e.getRequisitos().stream())
                .toList();

        Map<String, RequisitoDocumental> requisitosPorNombre = requisitos.stream()
                .collect(Collectors.toMap(
                        r -> normalizar(r.getTitulo()),
                        r -> r,
                        (r1, r2) -> r1
                ));

        Map<String, Documento> documentosPorEntidadId = new HashMap<>();
        cargarDocumentosRequisitos(requisitos, documentosPorEntidadId);
        cargarDocumentosHitos(hitos, documentosPorEntidadId);
        cargarDocumentoCarta(carta, documentosPorEntidadId);

        List<CreditoHipotecarioResponse.Item> items = new ArrayList<>();

        items.add(crearItem(
                "Carta de aprobación",
                hitosPorNombre.get(normalizar("Carta de aprobación del banco")),
                carta.getFechaEmision(),
                BigDecimal.ZERO,
                buscarDocumento("CARTA_APROBACION", carta.getId().toString(), documentosPorEntidadId)
        ));

        items.add(crearItem(
                "Pago de separación",
                hitosPorNombre.get(normalizar("Pago de separación")),
                null,
                carta.getPagoSeparacion(),
                buscarDocumentoEnRequisito(requisitosPorNombre, "Comprobante de separación", documentosPorEntidadId)
        ));

        HitoProcesoCompra hitoPagoInicial = hitosPorNombre.get(normalizar("Pago de la cuota inicial"));
        items.add(crearItem(
                "Pago inicial",
                hitoPagoInicial,
                null,
                carta.getPagoInicial(),
                hitoPagoInicial != null
                        ? buscarDocumento("HITO_PROCESO_COMPRA", hitoPagoInicial.getUuidHitoComercial().toString(), documentosPorEntidadId)
                        : null
        ));

        items.add(crearItem(
                "Desembolso",
                hitosPorNombre.get(normalizar("Desembolso Completado")),
                carta.getFechaDesembolsoProyectada(),
                carta.getMontoAprobado(),
                buscarDocumentoEnRequisito(requisitosPorNombre, "Inicio De desembolso", documentosPorEntidadId)
        ));

        // Fallback: hitos duplicados en etapa PAGO (hipotecario)
        CreditoHipotecarioResponse.Item itemSep = items.get(1);
        if (itemSep.fecha() == null && itemSep.estado().equals("PENDIENTE")) {
            HitoProcesoCompra hitoPagoSep = hitosPorNombre.get(normalizar("Pago de Separación"));
            if (hitoPagoSep != null) {
                items.set(1, crearItem(
                        "Pago de separación", hitoPagoSep, null,
                        carta.getPagoSeparacion(),
                        buscarDocumentoEnRequisito(requisitosPorNombre, "Comprobante de separación", documentosPorEntidadId)
                ));
            }
        }

        CreditoHipotecarioResponse.Item itemInicial = items.get(2);
        if (itemInicial.fecha() == null && itemInicial.estado().equals("PENDIENTE")) {
            HitoProcesoCompra hitoInicialPago = hitosPorNombre.get(normalizar("Pago Inicial"));
            if (hitoInicialPago != null) {
                items.set(2, crearItem(
                        "Pago inicial", hitoInicialPago, null,
                        carta.getPagoInicial(),
                        buscarDocumento("HITO_PROCESO_COMPRA", hitoInicialPago.getUuidHitoComercial().toString(), documentosPorEntidadId)
                ));
            }
        }

        BigDecimal montoTotal = items.stream()
                .map(CreditoHipotecarioResponse.Item::monto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completados = items.stream()
                .filter(i -> "COMPLETADO".equals(i.estado()))
                .count();

        double progreso = items.isEmpty() ? 0.0
                : Math.round((double) completados / items.size() * 100.0 * 100.0) / 100.0;

        log.info("Resumen crédito hipotecario generado para expediente: {}", uuidUsuarioActivo);
        return new CreditoHipotecarioResponse(items, montoTotal, progreso);
    }

    private CreditoHipotecarioResponse.Item crearItem(
            String nombre,
            HitoProcesoCompra hito,
            LocalDate fechaFallback,
            BigDecimal monto,
            Documento documento
    ) {
        String estado = hito != null ? hito.getEstado().name() : "PENDIENTE";
        LocalDate fecha = hito != null && hito.getFechaCompletado() != null
                ? hito.getFechaCompletado().toLocalDate()
                : fechaFallback;

        UUID docId = documento != null ? documento.getId() : null;
        String downloadUrl = null;
        if (docId != null) {
            try {
                downloadUrl = documentoService.generarSignedUrl(docId).url();
            } catch (Exception e) {
                log.warn("No se pudo generar signed URL para documento {}: {}", docId, e.getMessage());
            }
        }

        return new CreditoHipotecarioResponse.Item(nombre, fecha, estado, monto, docId, downloadUrl);
    }

    private void cargarDocumentosRequisitos(
            List<RequisitoDocumental> requisitos,
            Map<String, Documento> mapa
    ) {
        List<String> ids = requisitos.stream()
                .map(r -> r.getId().toString())
                .toList();

        List<Documento> docs = documentoRepository
                .findByEntidadReferenciaAndIdReferenciaInOrderByCreatedAtDesc(
                        ENTIDAD_REFERENCIA_REQUISITO, ids);

        for (Documento doc : docs) {
            String key = ENTIDAD_REFERENCIA_REQUISITO + "::" + doc.getIdReferencia();
            mapa.putIfAbsent(key, doc);
        }
    }

    private void cargarDocumentoCarta(
            CartaAprobacion carta,
            Map<String, Documento> mapa
    ) {
        String key = "CARTA_APROBACION::" + carta.getId().toString();
        documentoRepository
                .findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                        "CARTA_APROBACION", carta.getId().toString())
                .ifPresent(doc -> mapa.putIfAbsent(key, doc));
    }

    private void cargarDocumentosHitos(
            List<HitoProcesoCompra> hitos,
            Map<String, Documento> mapa
    ) {
        List<String> ids = hitos.stream()
                .map(h -> h.getUuidHitoComercial().toString())
                .toList();

        List<Documento> docs = documentoRepository
                .findByEntidadReferenciaAndIdReferenciaInOrderByCreatedAtDesc(
                        "HITO_PROCESO_COMPRA", ids);

        for (Documento doc : docs) {
            String key = "HITO_PROCESO_COMPRA::" + doc.getIdReferencia();
            mapa.putIfAbsent(key, doc);
        }
    }

    private Documento buscarDocumento(String entidad, String idRef, Map<String, Documento> mapa) {
        return mapa.get(entidad + "::" + idRef);
    }

    private Documento buscarDocumentoEnRequisito(
            Map<String, RequisitoDocumental> requisitos,
            String nombreRequisito,
            Map<String, Documento> documentos
    ) {
        RequisitoDocumental req = requisitos.get(normalizar(nombreRequisito));
        if (req == null) return null;
        return buscarDocumento(ENTIDAD_REFERENCIA_REQUISITO, req.getId().toString(), documentos);
    }

    private String normalizar(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private UsuarioActivo validarAcceso(String firebaseUid, UUID uuidUsuarioActivo) {
        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(uuidUsuarioActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Expediente no encontrado: " + uuidUsuarioActivo));

        boolean tieneAcceso = usuarioActivo.getClientes().stream()
                .anyMatch(c -> c.getId().equals(usuario.getId()));

        if (!tieneAcceso && "CLIENTE".equals(usuario.getTipoUsuario())) {
            throw new AccessDeniedException("No tienes acceso a este expediente");
        }

        return usuarioActivo;
    }
}
