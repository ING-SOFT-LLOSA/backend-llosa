package com.llosa.backend.proyecto.controller;


import com.llosa.backend.proyecto.dto.request.TorreRequestDTO;
import com.llosa.backend.proyecto.dto.response.TorreResponseDTO;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.service.TorreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TorreController {

    private final TorreService torreService;

    @PostMapping("/proyectos/{proyectoId}/torres")
    public ResponseEntity<TorreResponseDTO> save(@PathVariable UUID proyectoId,
                                                 @RequestBody TorreRequestDTO dto) {
        Torre torre = Torre.builder().nombre(dto.nombre()).build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TorreResponseDTO.fromEntity(torreService.save(proyectoId, torre)));
    }

    @GetMapping("/proyectos/{proyectoId}/torres")
    public ResponseEntity<List<TorreResponseDTO>> findByProyecto(@PathVariable UUID proyectoId) {
        List<TorreResponseDTO> response = torreService.findByProyectoId(proyectoId)
                .stream()
                .map(TorreResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/torres/{id}")
    public ResponseEntity<TorreResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(TorreResponseDTO.fromEntity(torreService.findById(id)));
    }

    @PutMapping("/torres/{id}")
    public ResponseEntity<TorreResponseDTO> update(@PathVariable Long id,
                                                    @RequestBody TorreRequestDTO dto) {
        Torre datos = Torre.builder().nombre(dto.nombre()).build();
        return ResponseEntity.ok(TorreResponseDTO.fromEntity(torreService.update(id, datos)));
    }

    @DeleteMapping("/torres/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        torreService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
