package com.llosa.backend.pagos.controller;

import com.llosa.backend.pagos.dto.CartaAprobacionRequest;
import com.llosa.backend.pagos.dto.CartaAprobacionResponse;
import com.llosa.backend.pagos.service.CartaAprobacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cartas-aprobacion")
@RequiredArgsConstructor
public class CartaAprobacionController {

    private final CartaAprobacionService cartaAprobacionService;

    @PostMapping
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<CartaAprobacionResponse> crear(@Valid @RequestBody CartaAprobacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartaAprobacionService.crear(request));
    }

    @GetMapping("/{uuidUsuarioActivo}")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<CartaAprobacionResponse> obtenerPorExpediente(@PathVariable UUID uuidUsuarioActivo) {
        return ResponseEntity.ok(cartaAprobacionService.obtenerPorUsuarioActivo(uuidUsuarioActivo));
    }

    @PutMapping("/{uuidCarta}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<CartaAprobacionResponse> actualizar(
            @PathVariable UUID uuidCarta,
            @Valid @RequestBody CartaAprobacionRequest request) {
        return ResponseEntity.ok(cartaAprobacionService.actualizar(uuidCarta, request));
    }

    @DeleteMapping("/{uuidCarta}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID uuidCarta) {
        cartaAprobacionService.eliminar(uuidCarta);
        return ResponseEntity.noContent().build();
    }
}
