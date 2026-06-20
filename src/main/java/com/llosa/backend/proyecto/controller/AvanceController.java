package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.HitoPisoUpdateDTO;
import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponseDTO;
import com.llosa.backend.proyecto.service.HitoPisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/avances-unidad")
@RequiredArgsConstructor
public class AvanceController {

    private final HitoPisoService hitoPisoService;

    /*
    Endpoint para actualizar un hitoPiso como completado
    Estado: Funcional
     */
    @PreAuthorize("hasAuthority('OBRA_EDITAR')")
    @PutMapping("/{idHitoPiso}")
    public ResponseEntity<AvanceUnidadResponseDTO> actualizarAvance(@PathVariable UUID idHitoPiso,
                                                                    @Valid @RequestBody HitoPisoUpdateDTO dto) {
        return ResponseEntity.ok(
            AvanceUnidadResponseDTO.fromEntity(hitoPisoService.cambiarEstado(idHitoPiso, dto.estado()))
        );
    }
}
