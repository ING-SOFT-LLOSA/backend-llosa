package com.llosa.backend.proyecto.controller;


import com.llosa.backend.proyecto.dto.request.HitoRequestDTO;
import com.llosa.backend.proyecto.dto.response.HitoResponseDTO;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.service.HitoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hitos")
@RequiredArgsConstructor
public class HitoController {

    private final HitoService hitoService;

    @GetMapping("/{id}")
    public ResponseEntity<HitoResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(HitoResponseDTO.fromEntity(hitoService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HitoResponseDTO> update(@PathVariable UUID id,
                                                   @RequestBody HitoRequestDTO dto) {
        Hito datos = Hito.builder()
                .titulo(dto.titulo())
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .estado(dto.estado())
                .fechaEstimada(dto.fechaEstimada())
                .build();
        return ResponseEntity.ok(HitoResponseDTO.fromEntity(hitoService.update(id, datos)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<HitoResponseDTO> cambiarEstado(@PathVariable UUID id,
                                                          @RequestParam EstadoHito estado) {
        return ResponseEntity.ok(HitoResponseDTO.fromEntity(hitoService.cambiarEstado(id, estado)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        hitoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
