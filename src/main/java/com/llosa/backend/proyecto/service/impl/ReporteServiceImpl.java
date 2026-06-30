package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.proyecto.dto.request.ReporteCreateRequest;
import com.llosa.backend.proyecto.dto.request.ReporteUpdateRequest;
import com.llosa.backend.proyecto.dto.response.ReporteResponse;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Reporte;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.ReporteRepository;
import com.llosa.backend.proyecto.service.ReporteService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private static final String ENTIDAD_REPORTE = "REPORTE";

    private final ReporteRepository reporteRepository;
    private final ProyectoRepository proyectoRepository;
    private final HitoRepository hitoRepository;
    private final DocumentoService documentoService;

    @Override
    @Transactional
    public ReporteResponse crear(ReporteCreateRequest request, List<MultipartFile> archivos, Integer usuarioId) {
        Proyecto proyecto = proyectoRepository.findById(request.uuidProyecto())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Proyecto no encontrado: " + request.uuidProyecto()));

        Reporte reporte = Reporte.builder()
                .proyecto(proyecto)
                .tituloPeriodo(request.tituloPeriodo())
                .porcentajeAvance(calcularAvance(request.uuidProyecto()))
                .descripcion(request.descripcion())
                .fecha(request.fecha())
                .hitosConsolidados(request.hitosConsolidados() != null
                        ? new ArrayList<>(request.hitosConsolidados())
                        : new ArrayList<>())
                .build();

        Reporte guardado = reporteRepository.saveAndFlush(reporte);

        // ── SUBIDA DE MULTIMEDIA A GCS ──
        if (archivos != null && !archivos.isEmpty()) {
            for (MultipartFile archivo : archivos) {

                // 1. Detectamos qué tipo de archivo es
                String contentType = archivo.getContentType();
                TipoDocumento tipoAsignado = TipoDocumento.FOTO_OBRA; // Por defecto

                if (contentType != null) {
                    if (contentType.startsWith("video/")) {
                        tipoAsignado = TipoDocumento.VIDEO_OBRA; // Asegúrate de tener este Enum
                    } else if (contentType.equals("application/pdf")) {
                        tipoAsignado = TipoDocumento.PDF_LEGAL; // Asegúrate de tener este Enum
                    }
                }
                documentoService.subirDocumentoPolimorfico(
                        archivo,
                        tipoAsignado,
                        guardado.getId().toString(),
                        ENTIDAD_REPORTE,
                        usuarioId
                );
            }
        }

        // Retornamos el reporte junto con sus imágenes firmadas
        List<DocumentoResponse> multimedia = documentoService.obtenerPorReferencia(ENTIDAD_REPORTE, guardado.getId().toString());
        return ReporteResponse.fromEntity(guardado, multimedia);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse obtenerPorId(UUID id) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reporte no encontrado: " + id));
        List<DocumentoResponse> multimedia =
                documentoService.obtenerPorReferencia(ENTIDAD_REPORTE, id.toString());

        return ReporteResponse.fromEntity(reporte, multimedia);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReporteResponse> listarPorProyecto(UUID uuidProyecto, Pageable pageable) {
        if (!proyectoRepository.existsById(uuidProyecto)) {
            throw new EntityNotFoundException("Proyecto no encontrado: " + uuidProyecto);
        }

        return reporteRepository
                .findByProyectoId(uuidProyecto, pageable)
                .map(reporte -> {
                    List<DocumentoResponse> multimedia = documentoService
                            .obtenerPorReferencia(ENTIDAD_REPORTE, reporte.getId().toString());
                    return ReporteResponse.fromEntity(reporte, multimedia);
                });
    }

    @Override
    @Transactional
    public ReporteResponse actualizar(UUID id, ReporteUpdateRequest request) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reporte no encontrado: " + id));

        reporte.setTituloPeriodo(request.tituloPeriodo());
        reporte.setPorcentajeAvance(calcularAvance(reporte.getProyecto().getId()));
        reporte.setDescripcion(request.descripcion());
        reporte.setFecha(request.fecha());
        reporte.getHitosConsolidados().clear();
        if (request.hitosConsolidados() != null) {
            reporte.getHitosConsolidados().addAll(request.hitosConsolidados());
        }
        Reporte actualizado = reporteRepository.save(reporte);
        List<DocumentoResponse> multimedia = documentoService.obtenerPorReferencia(ENTIDAD_REPORTE, actualizado.getId().toString());
        return ReporteResponse.fromEntity(actualizado, multimedia);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        if (!reporteRepository.existsById(id)) {
            throw new EntityNotFoundException("Reporte no encontrado: " + id);
        }
        documentoService.eliminarDocumentosPorReferencia(ENTIDAD_REPORTE, id.toString());
        reporteRepository.deleteById(id);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private BigDecimal calcularAvance(UUID proyectoId) {
        long total = hitoRepository.countByProyectoId(proyectoId);
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        long completados = hitoRepository.countByProyectoIdAndEstado(proyectoId, EstadoHito.COMPLETADO);
        return BigDecimal.valueOf(completados)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
