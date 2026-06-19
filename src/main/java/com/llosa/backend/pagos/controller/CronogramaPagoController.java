package com.llosa.backend.pagos.controller;

import com.llosa.backend.pagos.dto.CronogramaPagoRequest;
import com.llosa.backend.pagos.dto.CronogramaPagoResponse;
import com.llosa.backend.pagos.dto.ResumenResponse;
import com.llosa.backend.pagos.dto.ResumenResponseHipotecarioDTO;
import com.llosa.backend.pagos.service.CronogramaPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cronogramas")
@RequiredArgsConstructor
public class CronogramaPagoController {

    private final CronogramaPagoService cronogramaPagoService;

    @PostMapping
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<CronogramaPagoResponse> crear(@Valid @RequestBody CronogramaPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cronogramaPagoService.crear(request));
    }

    @GetMapping("/{uuidUsuarioActivo}")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<CronogramaPagoResponse> obtenerPorExpediente(@PathVariable UUID uuidUsuarioActivo) {
        return ResponseEntity.ok(cronogramaPagoService.obtenerPorUsuarioActivo(uuidUsuarioActivo));
    }

    @PutMapping("/{uuidCronograma}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<CronogramaPagoResponse> actualizar(
            @PathVariable UUID uuidCronograma,
            @Valid @RequestBody CronogramaPagoRequest request) {
        return ResponseEntity.ok(cronogramaPagoService.actualizar(uuidCronograma, request));
    }

    @DeleteMapping("/{uuidCronograma}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID uuidCronograma) {
        cronogramaPagoService.eliminar(uuidCronograma);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{uuidCronograma}/resumen")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<ResumenResponse> obtenerResumen(@PathVariable UUID uuidCronograma) {
        return ResponseEntity.ok(cronogramaPagoService.obtenerResumen(uuidCronograma));
    }

    @GetMapping("/{uuidCronograma}/resumen/credito-hipo")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<ResumenResponseHipotecarioDTO> obtenerResumenHitpotecario(@PathVariable UUID uuidCronograma) {
        return ResponseEntity.ok(cronogramaPagoService.obtenerResumenHipotecario(uuidCronograma));
    }

}
