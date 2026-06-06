package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.dto.StageDocumentResponse;
import com.llosa.backend.comercial.dto.StageResponse;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageService {

    private final HitoProcesoCompraRepository hitoRepository;
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoRepository documentoRepository;

    private static final List<EtapaProceso> ORDEN_ETAPAS = Arrays.asList(EtapaProceso.values());
    private static final int TOTAL_STEPS = ORDEN_ETAPAS.size();

    /**
     * Endpoint 1 — GET /api/stage/{etapaProceso}
     * Devuelve el stepper de hitos de una etapa + stageDetails si es CONTRATO.
     */
    public StageResponse obtenerStage(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso) {

        UsuarioActivo usuarioActivo = validarAcceso(firebaseUid, uuidUsuarioActivo);

        List<HitoProcesoCompra> hitos = hitoRepository
                .findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(uuidUsuarioActivo)
                .stream()
                .filter(h -> h.getEtapaProceso() == etapaProceso)
                .toList();

        // stage info
        int stepIndex = ORDEN_ETAPAS.indexOf(etapaProceso) + 1;
        double progreso = calcularProgreso(hitos);

        StageResponse.StageInfo stageInfo = new StageResponse.StageInfo(
                uuidUsuarioActivo.toString(),
                etapaProceso.name().charAt(0) + etapaProceso.name().substring(1).toLowerCase(),
                stepIndex,
                TOTAL_STEPS,
                progreso
        );

        // stepper items
        List<StageResponse.StepperItem> stepperItems = hitos.stream()
                .map(h -> new StageResponse.StepperItem(
                        h.getNombreHito(),
                        h.getEstado().name(),
                        h.getOrden()
                ))
                .toList();

        // stageDetails solo para CONTRATO
        StageResponse.StageDetails stageDetails = null;
        if (etapaProceso == EtapaProceso.CONTRATO) {
            Activo activo = usuarioActivo.getActivo();
            stageDetails = new StageResponse.StageDetails(
                    activo.getAreaM2() != null ? activo.getAreaM2() + " m2" : null,
                    activo.getPrecio() != null ? "S/. " + formatearPrecio(activo.getPrecio()) : null,
                    usuarioActivo.getFechaAdquisicion() != null
                            ? usuarioActivo.getFechaAdquisicion().toLocalDate().toString()
                            : null,
                    null // disbursementDate: pendiente de implementar (ver doc)
            );
        }

        return new StageResponse(stageInfo, stepperItems, stageDetails);
    }

    /**
     * Endpoint 2 — GET /api/stage/{etapaProceso}/documents
     * Devuelve los documentos asociados al expediente para una etapa dada.
     */
    public StageDocumentResponse obtenerDocumentosStage(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso) {

        validarAcceso(firebaseUid, uuidUsuarioActivo);

        List<Documento> documentos = documentoRepository
                .findByIdReferenciaAndEntidadReferencia(
                        uuidUsuarioActivo.toString(),
                        "USUARIO_ACTIVO"
                );

        List<StageDocumentResponse.DocumentoItem> items = documentos.stream()
                .map(doc -> new StageDocumentResponse.DocumentoItem(
                        doc.getId().toString(),
                        doc.getNombreOriginal(),
                        resolverDescripcion(doc),
                        doc.getCreatedAt() != null ? "completed" : "pending",
                        doc.getCreatedAt() != null ? doc.getCreatedAt().toLocalDate().toString() : null,
                        true,
                        "/api/documentos/" + doc.getId() + "/signed-url",
                        true,
                        null,
                        resolverIcono(doc)
                ))
                .toList();

        return new StageDocumentResponse(
                "Documentos — " + etapaProceso.name(),
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

        // Admins y empleados tienen acceso directo
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

    private String resolverDescripcion(Documento doc) {
        return switch (doc.getTipoDocumento()) {
            case PDF_LEGAL -> "Documento legal en formato PDF";
            case COMPROBANTE -> "Comprobante de pago";
            case FOTO_OBRA -> "Fotografía de avance de obra";
            case VIDEO_OBRA -> "Video de avance de obra";
        };
    }

    private String resolverIcono(Documento doc) {
        return switch (doc.getTipoDocumento()) {
            case PDF_LEGAL -> "contract";
            case COMPROBANTE -> "bank";
            case FOTO_OBRA -> "clipboard";
            case VIDEO_OBRA -> "file";
        };
    }
}
