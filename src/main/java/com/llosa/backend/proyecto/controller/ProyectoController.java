package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.EtapaCreateDTO;
import com.llosa.backend.proyecto.dto.response.DashboardProyectoDTO;
import com.llosa.backend.proyecto.dto.response.EtapaResponseDTO;
import com.llosa.backend.proyecto.dto.response.ProyectoResponseDTO;
import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.enums.EstadoEtapa;
import com.llosa.backend.proyecto.service.EtapaService;
import com.llosa.backend.proyecto.service.ProyectoService;
import jakarta.validation.Valid;
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

    @PostMapping("/{uuid}/etapas")
    public ResponseEntity<EtapaResponseDTO> crearEtapa(@PathVariable("uuid") UUID id,
                                                       @Valid @RequestBody EtapaCreateDTO dto) {
        Etapa etapa = Etapa.builder()
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .orden(dto.orden())
                .estado(EstadoEtapa.PENDIENTE)
                .build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EtapaResponseDTO.fromEntity(etapaService.save(id, etapa)));
    }

    @GetMapping("/{uuid}/avance-general")
    public ResponseEntity<DashboardProyectoDTO> getAvanceGeneral(@PathVariable("uuid") UUID id) {
        Proyecto proyecto = proyectoService.findById(id);
        double avance = proyectoService.getPorcentajeAvance(id);
        return ResponseEntity.ok(new DashboardProyectoDTO(proyecto.getId(), proyecto.getNombre(), avance));
    }
}
