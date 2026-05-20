package com.llosa.backend.proyecto.controller;


import com.llosa.backend.proyecto.dto.request.EtapaRequestDTO;
import com.llosa.backend.proyecto.dto.request.HitoRequestDTO;
import com.llosa.backend.proyecto.dto.response.EtapaResponseDTO;
import com.llosa.backend.proyecto.dto.response.HitoResponseDTO;
import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.enums.EstadoEtapa;
import com.llosa.backend.proyecto.service.EtapaService;
import com.llosa.backend.proyecto.service.HitoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/etapas")
@RequiredArgsConstructor
public class EtapaController {

    private final EtapaService etapaService;
    private final HitoService hitoService;

    @GetMapping("/{id}")
    public ResponseEntity<EtapaResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(EtapaResponseDTO.fromEntity(etapaService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EtapaResponseDTO> update(@PathVariable Long id,
                                                    @RequestBody EtapaRequestDTO dto) {
        Etapa datos = Etapa.builder()
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .estado(dto.estado())
                .build();
        return ResponseEntity.ok(EtapaResponseDTO.fromEntity(etapaService.update(id, datos)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<EtapaResponseDTO> cambiarEstado(@PathVariable Long id,
                                                           @RequestParam EstadoEtapa estado) {
        return ResponseEntity.ok(EtapaResponseDTO.fromEntity(etapaService.cambiarEstado(id, estado)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        etapaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // POST /api/etapas/{id}/hitos
    // -------------------------------------------------------------------------
    @PostMapping("/{id}/hitos")
    public ResponseEntity<HitoResponseDTO> crearHito(@PathVariable Long id,
                                                      @RequestBody HitoRequestDTO dto) {
        Hito hito = Hito.builder()
                .titulo(dto.titulo())
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .estado(dto.estado())
                .fechaEstimada(dto.fechaEstimada())
                .build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(HitoResponseDTO.fromEntity(hitoService.save(id, hito)));
    }

    @GetMapping("/{id}/hitos")
    public ResponseEntity<List<HitoResponseDTO>> findHitos(@PathVariable Long id) {
        List<HitoResponseDTO> response = hitoService.findByEtapaId(id)
                .stream()
                .map(HitoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
