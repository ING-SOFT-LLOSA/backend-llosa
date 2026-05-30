package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.HitoCreateDTO;
import com.llosa.backend.proyecto.dto.response.HitoResponseDTO;
import com.llosa.backend.proyecto.entity.Hito;
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

    @PutMapping("/{id}")
    public ResponseEntity<HitoResponseDTO> actualizarHito(@PathVariable("id") UUID uuid, @RequestBody HitoCreateDTO dto) {
        Hito hitoActualizado = hitoService.findById(uuid);
        hitoActualizado.setTitulo(dto.titulo());
        hitoActualizado.setOrden(dto.orden());
        hitoActualizado.setTipo(dto.tipo());
        hitoActualizado.setFechaCompletado(dto.fechaCompletado());
        return ResponseEntity.ok(HitoResponseDTO.fromEntity(hitoService.save(hitoActualizado)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHito(@PathVariable("id") UUID uuid) {
        hitoService.deleteById(uuid);
        return ResponseEntity.noContent().build();
    }
}
