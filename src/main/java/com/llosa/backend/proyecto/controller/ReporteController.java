package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.ReporteCreateRequest;
import com.llosa.backend.proyecto.dto.request.ReporteUpdateRequest;
import com.llosa.backend.proyecto.dto.response.ReporteResponse;
import com.llosa.backend.proyecto.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    /**
     * Crea un nuevo reporte de avance de obra.
     */
    @PostMapping
    public ResponseEntity<ReporteResponse> crear(@Valid @RequestBody ReporteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reporteService.crear(request));
    }

    /**
     * Obtiene un reporte por su ID. Incluye la lista de multimedia (fotos/videos)
     * asociados via la tabla polimórfica de Documentos.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReporteResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(reporteService.obtenerPorId(id));
    }

    /**
     * Lista todos los reportes de un proyecto con soporte de paginación.
     * Filtra directamente por uuid_proyecto para mayor velocidad.
     */
    @GetMapping("/proyecto/{uuidProyecto}")
    public ResponseEntity<Page<ReporteResponse>> listarPorProyecto(
            @PathVariable UUID uuidProyecto,
            Pageable pageable) {
        return ResponseEntity.ok(reporteService.listarPorProyecto(uuidProyecto, pageable));
    }

    /**
     * Actualiza título, avance, descripción, hitos y pisos de un reporte existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReporteResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ReporteUpdateRequest request) {
        return ResponseEntity.ok(reporteService.actualizar(id, request));
    }

    /**
     * Elimina un reporte por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        reporteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
