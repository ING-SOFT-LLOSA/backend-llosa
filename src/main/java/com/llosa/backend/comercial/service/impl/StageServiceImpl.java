package com.llosa.backend.comercial.service.impl;

import com.llosa.backend.comercial.dto.StageDocumentResponse;
import com.llosa.backend.comercial.dto.StageResponse;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageServiceImpl implements StageService {

    private final HitoProcesoCompraRepository hitoRepository;
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoRepository documentoRepository;
    private final RequisitoDocumentalRepository requisitoDocumentalRepository;
    private final DocumentoService documentoService;

    private static final String ENTIDAD_REFERENCIA_REQUISITO = "REQUISITO";
    private static final DateTimeFormatter FRONT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private static final List<EtapaProceso> ORDEN_ETAPAS = Arrays.asList(EtapaProceso.values());
    private static final int TOTAL_STEPS = ORDEN_ETAPAS.size();

    @Override
    public StageResponse obtenerStage(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso) {

        UsuarioActivo usuarioActivo = validarAcceso(firebaseUid, uuidUsuarioActivo);

        List<HitoProcesoCompra> hitos = hitoRepository
                .findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(uuidUsuarioActivo)
                .stream()
                .filter(h -> h.getEtapaProceso() == etapaProceso)
                .toList();

        int stepIndex = ORDEN_ETAPAS.indexOf(etapaProceso) + 1;
        double progreso = calcularProgreso(hitos);

        StageResponse.StageInfo stageInfo = new StageResponse.StageInfo(
                uuidUsuarioActivo.toString(),
                etapaProceso.name().charAt(0) + etapaProceso.name().substring(1).toLowerCase(),
                stepIndex,
                TOTAL_STEPS,
                progreso
        );

        List<StageResponse.StepperItem> stepperItems = hitos.stream()
                .map(h -> new StageResponse.StepperItem(
                        h.getNombreHito(),
                        h.getEstado().name(),
                        h.getOrden()
                ))
                .toList();

        StageResponse.StageDetails stageDetails = null;
        if (etapaProceso == EtapaProceso.CONTRATO) {
            Activo activo = usuarioActivo.getActivo();
            stageDetails = new StageResponse.StageDetails(
                    activo.getAreaM2() != null ? activo.getAreaM2() + " m2" : null,
                    activo.getPrecio() != null ? "S/. " + formatearPrecio(activo.getPrecio()) : null,
                    usuarioActivo.getFechaAdquisicion() != null
                            ? usuarioActivo.getFechaAdquisicion().toLocalDate().toString()
                            : null,
                    null
            );
        }

        return new StageResponse(stageInfo, stepperItems, stageDetails);
    }

    @Override
    public StageDocumentResponse obtenerDocumentosStage(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso) {
        validarAcceso(firebaseUid, uuidUsuarioActivo);

        HitoProcesoCompra hitoComercial = hitoRepository
                .findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUsuarioActivo, etapaProceso)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe hito comercial para uuidUsuarioActivo=" + uuidUsuarioActivo
                                + " y etapaProceso=" + etapaProceso));

        List<RequisitoDocumental> requisitos = requisitoDocumentalRepository
                .findByHitoComercial_UuidHitoComercialOrderByFechaEmisionDesc(hitoComercial.getUuidHitoComercial());

        List<StageDocumentResponse.DocumentoItem> items = requisitos.stream()
                .map(requisito -> {
                    Documento documento = documentoRepository
                            .findFirstByEntidadReferenciaAndIdReferenciaOrderByCreatedAtDesc(
                                    ENTIDAD_REFERENCIA_REQUISITO,
                                    requisito.getId().toString()
                            )
                            .orElse(null);

                    boolean hasDownload = documento != null;
                    String downloadUrl = null;

                    if (hasDownload) {
                        // generarSignedUrl solo recibe UUID ahora
                        SignedUrlResponse signed = documentoService.generarSignedUrl(documento.getId());
                        downloadUrl = signed.url();
                    }

                    String status = requisito.getEstado() != null
                            ? requisito.getEstado().toLowerCase(Locale.ROOT)
                            : "pendiente";

                    String emissionDate = requisito.getFechaEmision() != null
                            ? requisito.getFechaEmision().format(FRONT_DATE_FORMATTER)
                            : null;

                    return new StageDocumentResponse.DocumentoItem(
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

        return new StageDocumentResponse(
                "Documentos del " + etapaProceso.name().charAt(0) + etapaProceso.name().substring(1).toLowerCase(Locale.ROOT),
                items.size(),
                items
        );
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