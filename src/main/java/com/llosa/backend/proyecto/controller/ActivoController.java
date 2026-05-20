package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponseDTO;
import com.llosa.backend.proyecto.service.HitoUnidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivoController {

    private final HitoUnidadService hitoUnidadService;

    @GetMapping("/activos/{uuid_activo}/avances")
    public ResponseEntity<List<AvanceUnidadResponseDTO>> getAvances(@PathVariable("uuid_activo") UUID id) {
        List<AvanceUnidadResponseDTO> response = hitoUnidadService.findByActivo(id)
                .stream()
                .map(AvanceUnidadResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
