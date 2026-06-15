package com.llosa.backend.comercial.controller;

import com.llosa.backend.comercial.dto.EtapaExpedienteEstadoRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteResponse;
import com.llosa.backend.comercial.service.EtapaExpedienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/etapa-expediente")
@RequiredArgsConstructor
public class EtapaExpedienteController {

    private final EtapaExpedienteService etapaExpedienteService;

    @GetMapping("/expediente/{uuidUsuarioActivo}")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<List<EtapaExpedienteResponse>> listarPorExpediente(
            @PathVariable UUID uuidUsuarioActivo) {
        return ResponseEntity.ok(etapaExpedienteService.listarPorUsuarioActivo(uuidUsuarioActivo));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<EtapaExpedienteResponse> obtenerPorId(@PathVariable UUID uuid) {
        return ResponseEntity.ok(etapaExpedienteService.obtenerPorId(uuid));
    }

    @PostMapping("/expediente/{uuidUsuarioActivo}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<EtapaExpedienteResponse> crear(
            @PathVariable UUID uuidUsuarioActivo,
            @Valid @RequestBody EtapaExpedienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(etapaExpedienteService.crear(uuidUsuarioActivo, request));
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<EtapaExpedienteResponse> actualizar(
            @PathVariable UUID uuid,
            @Valid @RequestBody EtapaExpedienteRequest request) {
        return ResponseEntity.ok(etapaExpedienteService.actualizar(uuid, request));
    }

    @PatchMapping("/{uuid}/estado")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<EtapaExpedienteResponse> actualizarEstado(
            @PathVariable UUID uuid,
            @Valid @RequestBody EtapaExpedienteEstadoRequest request) {
        return ResponseEntity.ok(etapaExpedienteService.actualizarEstado(uuid, request));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID uuid) {
        etapaExpedienteService.eliminar(uuid);
        return ResponseEntity.noContent().build();
    }

}
