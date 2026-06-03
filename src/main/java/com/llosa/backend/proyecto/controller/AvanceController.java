package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.HitoUnidadUpdateDTO;
import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponseDTO;
import com.llosa.backend.proyecto.service.HitoUnidadService;
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

    private final HitoUnidadService hitoUnidadService;

    @PreAuthorize("hasAuthority('OBRA_EDITAR')")
    @PutMapping("/{id}")
    public ResponseEntity<AvanceUnidadResponseDTO> actualizarAvance(@PathVariable UUID id,
                                                                    @Valid @RequestBody HitoUnidadUpdateDTO dto) {
        return ResponseEntity.ok(
            AvanceUnidadResponseDTO.fromEntity(hitoUnidadService.cambiarEstado(id, dto.estado()))
        );
    }
}
