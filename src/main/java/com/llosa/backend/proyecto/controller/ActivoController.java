package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.ActivoRequestDTO;
import com.llosa.backend.proyecto.dto.response.*;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.HitoUnidad;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.HitoUnidadService;
import com.llosa.backend.proyecto.service.SeguimientoService;
import com.llosa.backend.proyecto.service.impl.SeguimientoServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivoController {

    private final HitoUnidadService hitoUnidadService;
    private final ActivoService activoService;
    private final SeguimientoService seguimientoService;

    // Consulta los hitos de un activo trayendote HITOUNIDAD
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @GetMapping("/activos/{id}/hitos")
    public ResponseEntity<List<HitoUnidadResponseDTO>> getActivos(@PathVariable UUID id) {
        List<HitoUnidad> hitoUnidades = hitoUnidadService.findByActivo(id);
        List<HitoUnidadResponseDTO> response = hitoUnidades.stream().map(
                HitoUnidadResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PROY_EDITAR')")
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

    @PreAuthorize("hasAuthority('PROY_EDITAR')")
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

    @PreAuthorize("hasAuthority('PROY_EDITAR')")
    @DeleteMapping("/activos/{id}")
    public ResponseEntity<Void> eliminarActivo(@PathVariable UUID id) {
        activoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Correctamente mapeado
    // Trae los activos que tiene un usario
    @PreAuthorize("hasAuthority('OBRA_VER')")
    @GetMapping("/activos/{uuid_activo}/avances")
    public ResponseEntity<List<AvanceUnidadResponseDTO>> getAvances(@PathVariable("uuid_activo") UUID id) {
        List<AvanceUnidadResponseDTO> response = hitoUnidadService.findByActivo(id)
                .stream()
                .map(AvanceUnidadResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping("/proyecto/{uuidProyecto}")
    public ResponseEntity<Page<ActivoResponseDTO>> listarActivosPorProyecto(
            @PathVariable UUID uuidProyecto,
            @RequestParam(required = false) EstadoComercialActivo estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivoResponseDTO> resultado = activoService.listarPorProyectoYEstado(uuidProyecto, estado, page, size);

        return ResponseEntity.ok(resultado);
    }

    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping("/{uuidActivo}/seguimiento")
    public ResponseEntity<SeguimientoResponseDTO> obtenerSeguimientoObra(@PathVariable UUID uuidActivo) {
        SeguimientoResponseDTO response = seguimientoService.obtenerSeguimiento(uuidActivo);
        return ResponseEntity.ok(response);
    }

}