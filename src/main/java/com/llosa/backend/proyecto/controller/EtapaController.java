package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.HitoCreateDTO;
import com.llosa.backend.proyecto.dto.response.HitoResponseDTO;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.service.HitoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/etapas")
@RequiredArgsConstructor
public class EtapaController {

    private final HitoService hitoService;

    @PostMapping("/{id}/hitos")
    public ResponseEntity<HitoResponseDTO> crearHito(@PathVariable Long id,
                                                      @Valid @RequestBody HitoCreateDTO dto) {
        Hito hito = Hito.builder()
                .titulo(dto.titulo())
                .orden(dto.orden())
                .tipo(dto.tipo())
                .estado(EstadoHito.PENDIENTE)
                .fechaCompletado(dto.fechaCompletado())
                .build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(HitoResponseDTO.fromEntity(hitoService.save(id, hito)));
    }
}
