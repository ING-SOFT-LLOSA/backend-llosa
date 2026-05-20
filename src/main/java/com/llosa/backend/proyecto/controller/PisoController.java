package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.PisoRequestDTO;
import com.llosa.backend.proyecto.dto.response.PisoResponseDTO;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.service.PisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PisoController {

    private final PisoService pisoService;

    @PostMapping("/torres/{torreId}/pisos")
    public ResponseEntity<PisoResponseDTO> save(@PathVariable Long torreId,
                                                 @RequestBody PisoRequestDTO dto) {
        Piso piso = Piso.builder().nroPiso(dto.nroPiso()).build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PisoResponseDTO.fromEntity(pisoService.save(torreId, piso)));
    }

    @GetMapping("/torres/{torreId}/pisos")
    public ResponseEntity<List<PisoResponseDTO>> findByTorre(@PathVariable Long torreId) {
        List<PisoResponseDTO> response = pisoService.findByTorreId(torreId)
                .stream()
                .map(PisoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pisos/{id}")
    public ResponseEntity<PisoResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(PisoResponseDTO.fromEntity(pisoService.findById(id)));
    }

    @PutMapping("/pisos/{id}")
    public ResponseEntity<PisoResponseDTO> update(@PathVariable Long id,
                                                   @RequestBody PisoRequestDTO dto) {
        Piso datos = Piso.builder().nroPiso(dto.nroPiso()).build();
        return ResponseEntity.ok(PisoResponseDTO.fromEntity(pisoService.update(id, datos)));
    }

    @DeleteMapping("/pisos/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pisoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
