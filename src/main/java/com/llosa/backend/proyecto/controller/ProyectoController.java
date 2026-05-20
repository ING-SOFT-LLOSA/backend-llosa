package com.llosa.backend.proyecto.controller;


import com.llosa.backend.proyecto.dto.request.EtapaRequestDTO;
import com.llosa.backend.proyecto.dto.request.ProyectoRequestDTO;
import com.llosa.backend.proyecto.dto.response.ArbolFisicoResponseDTO;
import com.llosa.backend.proyecto.dto.response.CronogramaResponseDTO;
import com.llosa.backend.proyecto.dto.response.EtapaResponseDTO;
import com.llosa.backend.proyecto.dto.response.ProyectoResponseDTO;
import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.service.EtapaService;
import com.llosa.backend.proyecto.service.ProyectoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/proyectos")
@RequiredArgsConstructor
public class ProyectoController {

    private final ProyectoService proyectoService;
    private final EtapaService etapaService;

    @GetMapping
    public ResponseEntity<List<ProyectoResponseDTO>> findAll() {
        List<ProyectoResponseDTO> response = proyectoService.findAll()
                .stream()
                .map(ProyectoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ProyectoResponseDTO> save(@RequestBody ProyectoRequestDTO dto) {
        Proyecto proyecto = Proyecto.builder()
                .nombre(dto.nombre())
                .distrito(dto.distrito())
                .direccion(dto.direccion())
                .fechaInicio(dto.fechaInicio())
                .build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ProyectoResponseDTO.fromEntity(proyectoService.save(proyecto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProyectoResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ProyectoResponseDTO.fromEntity(proyectoService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProyectoResponseDTO> update(@PathVariable UUID id,
                                                       @RequestBody ProyectoRequestDTO dto) {
        Proyecto datos = Proyecto.builder()
                .nombre(dto.nombre())
                .distrito(dto.distrito())
                .direccion(dto.direccion())
                .fechaInicio(dto.fechaInicio())
                .build();
        return ResponseEntity.ok(ProyectoResponseDTO.fromEntity(proyectoService.update(id, datos)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        proyectoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // POST /api/proyectos/{uuid}/etapas
    // -------------------------------------------------------------------------
    @PostMapping("/{id}/etapas")
    public ResponseEntity<EtapaResponseDTO> crearEtapa(@PathVariable UUID id,
                                                       @RequestBody EtapaRequestDTO dto) {
        Etapa etapa = Etapa.builder()
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .estado(dto.estado())
                .build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EtapaResponseDTO.fromEntity(etapaService.save(id, etapa)));
    }

    // -------------------------------------------------------------------------
    // GET /api/proyectos/{uuid}/cronograma  →  Proyecto → Etapas → Hitos
    // -------------------------------------------------------------------------
    @GetMapping("/{id}/cronograma")
    public ResponseEntity<CronogramaResponseDTO> cronograma(@PathVariable UUID id) {
        return ResponseEntity.ok(
                CronogramaResponseDTO.fromEntity(proyectoService.findCronograma(id))
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/proyectos/{uuid}/arbol-fisico  →  Proyecto → Torres → Pisos → Activos
    // -------------------------------------------------------------------------
    @GetMapping("/{id}/arbol-fisico")
    public ResponseEntity<ArbolFisicoResponseDTO> arbolFisico(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ArbolFisicoResponseDTO.fromEntity(proyectoService.findArbolFisico(id))
        );
    }
}
