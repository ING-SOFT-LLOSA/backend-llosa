package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.response.PisoResponseDTO;
import com.llosa.backend.proyecto.service.PisoService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pisos")
@RequiredArgsConstructor
@Validated
public class PisoController {

    private final PisoService pisoService;

    /*
    Endpoint Obtener los piso por torre_id
    Estado: Funcional
     */
    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping("/{id_torre}")
    public ResponseEntity<List<PisoResponseDTO>> getPisosByTorre(
            @PathVariable("id_torre") Long torreId,
            @RequestParam(required = false)
            @Size(max = 20, message = "El texto de búsqueda no puede superar los 20 caracteres")
            String search) {
        
        List<PisoResponseDTO> response = pisoService.findByTorre(torreId, search)
                .stream()
                .map(PisoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
