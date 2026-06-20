package com.llosa.backend.comercial.service.impl;

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
import com.llosa.backend.comercial.service.StageService;
import com.llosa.backend.documentos.dto.SignedUrlResponse;

import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageServiceImpl implements StageService {

    private final EtapaExpedienteRepository etapaExpedienteRepository;
    private final HitoProcesoCompraRepository hitoRepository;
    private final RequisitoDocumentalRepository requisitoDocumentalRepository;
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoService documentoService;

    private static final String ENTIDAD_REFERENCIA_REQUISITO = "REQUISITO";
    private static final DateTimeFormatter FRONT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private static final List<EtapaProceso> ORDEN_ETAPAS = Arrays.asList(EtapaProceso.values());
    private static final int TOTAL_STEPS = ORDEN_ETAPAS.size();

    @Override
    public StageTrackerResponse obtenerStage(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso) {

        UsuarioActivo usuarioActivo = validarAcceso(firebaseUid, uuidUsuarioActivo);

        EtapaExpediente etapa = etapaExpedienteRepository
                .findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUsuarioActivo, etapaProceso)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe etapa " + etapaProceso + " para el expediente " + uuidUsuarioActivo));

        List<HitoProcesoCompra> hitos = hitoRepository
                .findByEtapaExpediente_UuidEtapaExpedienteOrderByOrdenAsc(etapa.getUuidEtapaExpediente());

        int stepIndex = ORDEN_ETAPAS.indexOf(etapaProceso) + 1;
        double progreso = calcularProgreso(hitos);

        StageTrackerResponse.StageInfo stageInfo = new StageTrackerResponse.StageInfo(
                uuidUsuarioActivo.toString(),
                etapaProceso.name().charAt(0) + etapaProceso.name().substring(1).toLowerCase(),
                stepIndex,
                TOTAL_STEPS,
                progreso
        );

        List<StageTrackerResponse.StepperItem> stepperItems = hitos.stream()
                .map(h -> new StageTrackerResponse.StepperItem(
                        h.getNombreHito(),
                        h.getEstado().name(),
                        h.getOrden()
                ))
                .toList();

        StageTrackerResponse.StageDetails stageDetails = null;
        if (etapaProceso == EtapaProceso.CONTRATO) {
            List<Activo> activos = usuarioActivo.getActivos();
            BigDecimal areaTotal = activos.stream()
                    .map(a -> a.getAreaM2() != null ? a.getAreaM2() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal precioTotal = activos.stream()
                    .map(a -> a.getPrecio() != null ? a.getPrecio() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stageDetails = new StageTrackerResponse.StageDetails(
                    areaTotal.compareTo(BigDecimal.ZERO) > 0 ? areaTotal + " m2" : null,
                    precioTotal.compareTo(BigDecimal.ZERO) > 0 ? "S/. " + formatearPrecio(precioTotal) : null,
                    usuarioActivo.getFechaAdquisicion() != null
                            ? usuarioActivo.getFechaAdquisicion().toLocalDate().toString()
                            : null,
                    null
            );
        }

        return new StageTrackerResponse(stageInfo, stepperItems, stageDetails);
    }

    @Override
    public StageDocumentsResponse obtenerDocumentosStage(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso) {
        validarAcceso(firebaseUid, uuidUsuarioActivo);

        EtapaExpediente etapa = etapaExpedienteRepository
                .findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUsuarioActivo, etapaProceso)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe etapa " + etapaProceso + " para el expediente " + uuidUsuarioActivo));

        List<RequisitoDocumental> requisitos = requisitoDocumentalRepository
                .findByEtapaExpediente_UuidEtapaExpedienteOrderByFechaEmisionDesc(etapa.getUuidEtapaExpediente());

        List<String> requisitosIds = requisitos.stream()
                .map(r -> r.getId().toString())
                .toList();
        // Traemos TODOS los documentos asociados a estos requisitos en UNA SOLA QUERY SQL
        List<Documento> documentosAsociados = documentoRepository
                .findByEntidadReferenciaAndIdReferenciaInOrderByCreatedAtDesc(
                        ENTIDAD_REFERENCIA_REQUISITO,
                        requisitosIds
                );
        // Agrupamos los documentos en un Map (Key: idReferencia, Value: Documento)
        // Usamos un merge function (d1, d2) -> d1 para que, si hay duplicados, se quede con el primero (el más nuevo por el OrderBy)
        Map<String, Documento> documentosMap = documentosAsociados.stream()
                .collect(Collectors.toMap(
                        Documento::getIdReferencia,
                        d -> d,
                        (documentoMasNuevo, documentoAntiguo) -> documentoMasNuevo
                ));

        List<StageDocumentsResponse.DocumentoItem> items = requisitos.stream()
                .map(requisito -> {

                    // Buscamos en nuestro mapa de la RAM. ¡Ya no hay queries aquí adentro!
                    Documento documento = documentosMap.get(requisito.getId().toString());

                    String downloadUrl = null;
                    boolean hasDownload = documento != null;

                    if (hasDownload) {
                        SignedUrlResponse signed = documentoService.generarSignedUrl(documento.getId());
                        downloadUrl = signed.url();
                    }

                    String status = requisito.getEstado() != null
                            ? requisito.getEstado().name().toLowerCase(Locale.ROOT)
                            : "pendiente";

                    String emissionDate = requisito.getFechaEmision() != null
                            ? requisito.getFechaEmision().format(FRONT_DATE_FORMATTER)
                            : null;

                    return new StageDocumentsResponse.DocumentoItem(
                            requisito.getId().toString(),
                            requisito.getTitulo(),
                            requisito.getDescripcion(),
                            status,
                            emissionDate,
                            hasDownload,
                            downloadUrl,
                            true,
                            requisito.getNotaCorporativa(),
                            requisito.getIcono()
                    );
                })
                .toList();

        String nombreEtapaFormateado = etapaProceso.name().charAt(0) + etapaProceso.name().substring(1).toLowerCase(Locale.ROOT);
        return new StageDocumentsResponse(
                "Documentos de la etapa " + nombreEtapaFormateado,
                items.size(),
                items
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StageActivosResponse obtenerActivosEtapa(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso) {

        // 1. Si no estamos en la etapa de CONTRATO, retornamos estructura vacía de inmediato
        if (etapaProceso != EtapaProceso.CONTRATO) {
            return new StageActivosResponse(null, 0, List.of());
        }

        // 2. Validar seguridad y obtener el expediente
        UsuarioActivo ua = validarAcceso(firebaseUid, uuidUsuarioActivo);

        List<StageActivosResponse.ActivoItemResponse> activosDTO = new ArrayList<>();
        BigDecimal areaTotal = BigDecimal.ZERO;

        // 3. Recorrer los activos (Complejidad reducida usando 'continue')
        if (ua.getActivos() != null) {
            for (Activo activo : ua.getActivos()) {
                if (activo == null) {
                    continue; // Evita anidar el resto del código en un 'if'
                }

                activosDTO.add(construirActivoResponse(activo));

                if (activo.getAreaM2() != null) {
                    areaTotal = areaTotal.add(activo.getAreaM2());
                }
            }
        }

        // 4. Formatear el resumen delegando la lógica
        String resumen = construirResumenContrato(ua, areaTotal);

        // 5. Retornar
        return new StageActivosResponse(resumen, activosDTO.size(), activosDTO);
    }

    private StageActivosResponse.ActivoItemResponse construirActivoResponse(Activo activo) {
        String tipoNombre = activo.getTipo() != null ? activo.getTipo().name() : null;

        return new StageActivosResponse.ActivoItemResponse(
                activo.getId(), // 'id' mapeado a uuid_activo
                activo.getNro(),
                tipoNombre,
                activo.getAreaM2(),
                activo.getPrecio(),
                activo.getDescripcion(),
                activo.getLinkRecorridoVirtual()
        );
    }

    private String construirResumenContrato(UsuarioActivo ua, BigDecimal areaTotal) {
        String firmaContrato = ua.getFechaAdquisicion() != null
                ? ua.getFechaAdquisicion().format(FRONT_DATE_FORMATTER)
                : "Pendiente";

        String financiamiento = ua.getTipoFinanciamiento() != null
                ? ua.getTipoFinanciamiento()
                : "-";

        return String.format(Locale.US,
                "Contrato firmado: %s | Financiamiento: %s | Área total: %.2f m²",
                firmaContrato, financiamiento, areaTotal.doubleValue());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private UsuarioActivo validarAcceso(String firebaseUid, UUID uuidUsuarioActivo) {
        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(uuidUsuarioActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Expediente no encontrado: " + uuidUsuarioActivo));

        boolean tieneAcceso = usuarioActivo.getClientes().stream()
                .anyMatch(c -> c.getId().equals(usuario.getId()));

        if (!tieneAcceso && "CLIENTE".equals(usuario.getTipoUsuario())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes acceso a este expediente");
        }

        return usuarioActivo;
    }


    private double calcularProgreso(List<HitoProcesoCompra> hitos) {
        if (hitos.isEmpty()) return 0.0;
        long completados = hitos.stream()
                .filter(h -> h.getEstado() == EstadoHitoComercial.COMPLETADO)
                .count();
        return Math.round(((double) completados / hitos.size()) * 100.0 * 100.0) / 100.0;
    }

    private String formatearPrecio(BigDecimal precio) {
        return String.format("%,.2f", precio);
    }
}