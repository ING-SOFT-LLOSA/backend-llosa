package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.response.TorreResponseDTO;
import com.llosa.backend.proyecto.service.TorreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/torres")
@RequiredArgsConstructor
public class TorreController {

    private final TorreService torreService;

    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping("/{id_proyecto}")
    public ResponseEntity<List<TorreResponseDTO>> getTorresByProyecto(
            @PathVariable("id_proyecto") UUID proyectoId,
            @RequestParam(required = false) String search) {
        
        List<TorreResponseDTO> response = torreService.findByProyecto(proyectoId, search)
                .stream()
                .map(TorreResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
