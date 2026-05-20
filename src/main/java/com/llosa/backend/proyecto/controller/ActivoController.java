package com.llosa.backend.proyecto.controller;


import com.llosa.backend.proyecto.dto.request.ActivoRequestDTO;
import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.service.ActivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivoController {

    private final ActivoService activoService;

    @PostMapping("/pisos/{pisoId}/activos")
    public ResponseEntity<ActivoResponseDTO> save(@PathVariable Long pisoId,
                                                   @RequestBody ActivoRequestDTO dto) {
        Activo activo = Activo.builder()
                .nro(dto.nro())
                .tipo(dto.tipo())
                .precio(dto.precio())
                .descripcion(dto.descripcion())
                .build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ActivoResponseDTO.fromEntity(activoService.save(pisoId, activo)));
    }

    @GetMapping("/pisos/{pisoId}/activos")
    public ResponseEntity<List<ActivoResponseDTO>> findByPiso(@PathVariable Long pisoId) {
        List<ActivoResponseDTO> response = activoService.findByPisoId(pisoId)
                .stream()
                .map(ActivoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/proyectos/{proyectoId}/activos")
    public ResponseEntity<List<ActivoResponseDTO>> findByProyecto(@PathVariable UUID proyectoId) {
        List<ActivoResponseDTO> response = activoService.findByProyectoId(proyectoId)
                .stream()
                .map(ActivoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/activos/{id}")
    public ResponseEntity<ActivoResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ActivoResponseDTO.fromEntity(activoService.findById(id)));
    }

    @PutMapping("/activos/{id}")
    public ResponseEntity<ActivoResponseDTO> update(@PathVariable UUID id,
                                                     @RequestBody ActivoRequestDTO dto) {
        Activo datos = Activo.builder()
                .nro(dto.nro())
                .tipo(dto.tipo())
                .precio(dto.precio())
                .descripcion(dto.descripcion())
                .build();
        return ResponseEntity.ok(ActivoResponseDTO.fromEntity(activoService.update(id, datos)));
    }

    @DeleteMapping("/activos/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        activoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
