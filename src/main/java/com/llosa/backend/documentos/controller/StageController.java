package com.llosa.backend.documentos.controller;

import com.llosa.backend.comercial.dto.StageDocumentResponse;
import com.llosa.backend.comercial.dto.StageResponse;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.service.impl.StageServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stage")
@RequiredArgsConstructor
public class StageController {

    private final StageServiceImpl stageService;

    /**
     * Endpoint 1 — GET /api/stage/{etapaProceso}?uuidUsuarioActivo=xxx
     * Devuelve la info del stage: stepper de hitos + stageDetails (solo si es CONTRATO).
     */
    @GetMapping("/{etapaProceso}")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<StageResponse> obtenerStage(
            @PathVariable EtapaProceso etapaProceso,
            @RequestParam UUID uuidUsuarioActivo,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        return ResponseEntity.ok(
                stageService.obtenerStage(uid, uuidUsuarioActivo, etapaProceso)
        );
    }

    /**
     * Endpoint 2 — GET /api/stage/{etapaProceso}/documents?uuidUsuarioActivo=xxx
     * Devuelve los documentos del expediente para la etapa indicada.
     */
    @GetMapping("/{etapaProceso}/documents")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<StageDocumentResponse> obtenerDocumentosStage(
            @PathVariable EtapaProceso etapaProceso,
            @RequestParam UUID uuidUsuarioActivo,
            Authentication authentication
    ) {
        String uid = (String) authentication.getPrincipal();
        return ResponseEntity.ok(
                stageService.obtenerDocumentosStage(uid, uuidUsuarioActivo, etapaProceso)
        );
    }
}
