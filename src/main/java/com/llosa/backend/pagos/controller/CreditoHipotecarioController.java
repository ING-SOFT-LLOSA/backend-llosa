package com.llosa.backend.pagos.controller;

import com.llosa.backend.pagos.dto.CreditoHipotecarioResponse;
import com.llosa.backend.pagos.service.CreditoHipotecarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/credito-hipotecario")
@RequiredArgsConstructor
public class CreditoHipotecarioController {

    private final CreditoHipotecarioService creditoHipotecarioService;

    @GetMapping("/{uuidUsuarioActivo}")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<CreditoHipotecarioResponse> obtenerResumen(
            @PathVariable UUID uuidUsuarioActivo,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        return ResponseEntity.ok(creditoHipotecarioService.obtenerResumen(uuidUsuarioActivo, uid));
    }
}
