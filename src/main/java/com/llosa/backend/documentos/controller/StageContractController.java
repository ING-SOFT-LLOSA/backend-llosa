package com.llosa.backend.documentos.controller;

import com.llosa.backend.documentos.dto.StageDocumentResponse;
import com.llosa.backend.documentos.service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/stage")
@RequiredArgsConstructor
public class StageContractController {

    private final DocumentoService documentoService;

    /**
     * GET /stage/{etapaProceso}/{uuidUsuarioActivo}
     * Devuelve los detalles de un contrato inmobiliario para la etapa de contrato.
     */
    @GetMapping("/{etapaProceso}/{uuidUsuarioActivo}")
    public ResponseEntity<StageDocumentResponse> obtenerDetalleEtapa(
            @PathVariable String etapaProceso,
            @PathVariable UUID uuidUsuarioActivo
    ) {
        return ResponseEntity.ok(documentoService.obtenerDetalleEtapa(etapaProceso, uuidUsuarioActivo));
    }
}