package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.HitoPisoUpdateDTO;
import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponseDTO;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.service.HitoPisoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/avances-unidad")
@RequiredArgsConstructor
public class AvanceController {

    private final HitoPisoService hitoPisoService;

    @PreAuthorize("hasAuthority('OBRA_EDITAR')")
    @PutMapping("/{id}")
    public ResponseEntity<AvanceUnidadResponseDTO> actualizarAvance(@PathVariable UUID id,
                                                                    @Valid @RequestBody HitoPisoUpdateDTO dto) {
        return ResponseEntity.ok(
            AvanceUnidadResponseDTO.fromEntity(hitoPisoService.cambiarEstado(id, dto.estado()))
        );
    }

    // CP23: actualización masiva de un hito para toda una Torre
    @PreAuthorize("hasAuthority('OBRA_EDITAR')")
    @PutMapping("/torre/{torreId}/hito/{hitoId}")
    public ResponseEntity<List<AvanceUnidadResponseDTO>> actualizarAvancePorTorre(
            @PathVariable Long torreId,
            @PathVariable UUID hitoId,
            @Valid @RequestBody HitoPisoUpdateDTO dto) {
        List<HitoPiso> actualizados = hitoPisoService.cambiarEstadoPorTorre(torreId, hitoId, dto.estado());
        List<AvanceUnidadResponseDTO> response = actualizados.stream()
                .map(AvanceUnidadResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
