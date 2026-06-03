package com.llosa.backend.proyecto.controller.comercial;

import com.llosa.backend.proyecto.dto.comercial.HitoComercialRequest;
import com.llosa.backend.proyecto.dto.comercial.HitoComercialResponse;
import com.llosa.backend.proyecto.dto.comercial.StepperResponse;
import com.llosa.backend.proyecto.entity.comercial.EstadoHitoComercial;
import com.llosa.backend.proyecto.service.comercial.HitoComercialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la gestión de Hitos Comerciales (Proceso de Compra).
 * Base path: /api/comercial
 */
@RestController
@RequestMapping("/api/comercial")
@RequiredArgsConstructor
public class HitoComercialController {

    private final HitoComercialService hitoComercialService;

    /**
     * ENDPOINT 1: Crear un nuevo hito comercial.
     * POST /api/comercial/hitos
     */
    @PostMapping("/hitos")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<HitoComercialResponse> crearHito(@Valid @RequestBody HitoComercialRequest request) {
        HitoComercialResponse response = hitoComercialService.crearHito(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ENDPOINT 2: Eliminar un hito comercial.
     * DELETE /api/comercial/hitos/{uuid}
     */
    @DeleteMapping("/hitos/{uuid}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<Void> eliminarHito(@PathVariable UUID uuid) {
        hitoComercialService.eliminarHito(uuid);
        return ResponseEntity.noContent().build();
    }

    /**
     * ENDPOINT 3: Actualizar el estado de un hito comercial.
     * PATCH /api/comercial/hitos/{uuid}/estado
     */
    @PatchMapping("/hitos/{uuid}/estado")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<HitoComercialResponse> actualizarEstado(
            @PathVariable UUID uuid,
            @RequestParam EstadoHitoComercial estado) {
        HitoComercialResponse response = hitoComercialService.actualizarEstado(uuid, estado);
        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT 4: Obtener el stepper completo del proceso de compra para un UsuarioActivo.
     * GET /api/comercial/stepper/{uuidUsuarioActivo}
     */
    @GetMapping("/stepper/{uuidUsuarioActivo}")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<StepperResponse> obtenerStepper(@PathVariable UUID uuidUsuarioActivo) {
        StepperResponse response = hitoComercialService.obtenerStepper(uuidUsuarioActivo);
        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT 5: Inicializar hitos por defecto para un UsuarioActivo.
     * POST /api/comercial/stepper/{uuidUsuarioActivo}/inicializar
     */
    @PostMapping("/stepper/{uuidUsuarioActivo}/inicializar")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<StepperResponse> inicializarHitos(@PathVariable UUID uuidUsuarioActivo) {
        StepperResponse response = hitoComercialService.inicializarHitosPorDefecto(uuidUsuarioActivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
