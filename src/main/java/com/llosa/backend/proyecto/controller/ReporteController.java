package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.ReporteCreateRequest;
import com.llosa.backend.proyecto.dto.request.ReporteUpdateRequest;
import com.llosa.backend.proyecto.dto.response.ReporteResponse;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final ActivoService activoService;

    /**
     * Crea un nuevo reporte de avance de obra.
     */
    @PreAuthorize("hasAuthority('PROY_CREAR')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReporteResponse> crearReporte(
            @RequestPart("reporte") @Valid ReporteCreateRequest request,
            @RequestPart(value = "archivos", required = false) List<MultipartFile> archivos,
            Authentication authentication // Para sacar el ID del usuario
    ) {
        Integer usuarioId = (Integer) authentication.getPrincipal();
        ReporteResponse response = reporteService.crear(request, archivos, usuarioId);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene un reporte por su ID. Incluye la lista de multimedia (fotos/videos)
     * asociados via la tabla polimórfica de Documentos.
     */
    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping("/{id}")
    public ResponseEntity<ReporteResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(reporteService.obtenerPorId(id));
    }

    /**
     * Lista todos los reportes de un proyecto con soporte de paginación.
     * Filtra directamente por uuid_proyecto para mayor velocidad.
     */
    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping("/proyecto/{uuidProyecto}")
    public ResponseEntity<Page<ReporteResponse>> listarPorProyecto(
            @PathVariable UUID uuidProyecto,
            Pageable pageable) {
        return ResponseEntity.ok(reporteService.listarPorProyecto(uuidProyecto, pageable));
    }

    /**
     * Lista todos los reportes de un proyecto con soporte de paginación.
     * Filtra directamente por uuid_proyecto para mayor velocidad.
     */
    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping("/proyecto/{uuidActivo}/activo")
    public ResponseEntity<Page<ReporteResponse>> listarPorActivoProyecto(
            @PathVariable UUID uuidActivo,
            Pageable pageable) {
        Activo activo = activoService.findById(uuidActivo);
        UUID uuidProyecto = activo.getPiso().getTorre().getProyecto().getId();
        return ResponseEntity.ok(reporteService.listarPorProyecto(uuidProyecto, pageable));
    }

    /**
     * Actualiza título, avance, descripción, hitos y pisos de un reporte existente.
     */
    @PreAuthorize("hasAuthority('PROY_EDITAR')")
    @PutMapping("/{id}")
    public ResponseEntity<ReporteResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ReporteUpdateRequest request) {
        return ResponseEntity.ok(reporteService.actualizar(id, request));
    }

    /**
     * Elimina un reporte por su ID.
     */
    @PreAuthorize("hasAuthority('PROY_EDITAR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        reporteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
