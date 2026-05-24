package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.ActivoRequestDTO;
import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.service.ActivoService;
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
    private final ActivoService activoService;


    @PostMapping("/activos/{id}/pisos")
    public ResponseEntity<ActivoResponseDTO> crearActivo(@PathVariable Long id, @RequestBody ActivoRequestDTO activoDTO) {
        Activo activo = Activo.builder()
                .nro(activoDTO.nro())
                .tipo(activoDTO.tipo())
                .areaM2(activoDTO.areaM2())
                .estadoComercial(activoDTO.estadoComercial())
                .precio(activoDTO.precio())
                .descripcion(activoDTO.descripcion())
                .build();
        return ResponseEntity.ok(ActivoResponseDTO.fromEntity(activoService.saveIndividual(id, activo)));
    }

    @PutMapping("/activos/{id}")
    public ResponseEntity<ActivoResponseDTO> actualizarActivo(@PathVariable UUID id, @RequestBody ActivoRequestDTO activoDTO) {
        Activo activoExistente = activoService.findById(id);

        activoExistente.setNro(activoDTO.nro());
        activoExistente.setTipo(activoDTO.tipo());
        activoExistente.setAreaM2(activoDTO.areaM2());
        activoExistente.setEstadoComercial(activoDTO.estadoComercial());
        activoExistente.setPrecio(activoDTO.precio());
        activoExistente.setDescripcion(activoDTO.descripcion());

        Activo actualizado = activoService.save(activoExistente);

        return ResponseEntity.ok(ActivoResponseDTO.fromEntity(actualizado));
    }

    @DeleteMapping("/activos/{id}")
    public ResponseEntity<Void> eliminarActivo(@PathVariable UUID id) {
        activoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activos/{uuid_activo}/avances")
    public ResponseEntity<List<AvanceUnidadResponseDTO>> getAvances(@PathVariable("uuid_activo") UUID id) {
        List<AvanceUnidadResponseDTO> response = hitoUnidadService.findByActivo(id)
                .stream()
                .map(AvanceUnidadResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}
