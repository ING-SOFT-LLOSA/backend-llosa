package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.ActivoRequestDTO;
import com.llosa.backend.proyecto.dto.response.*;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.HitoPisoService;
import com.llosa.backend.proyecto.service.SeguimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/activos")
@RequiredArgsConstructor
public class ActivoController {

    private final HitoPisoService hitoPisoService;
    private final ActivoService activoService;
    private final SeguimientoService seguimientoService;

    /*
    Endpoint Obtener los activos por piso_id
    Estado: Funcional
     */
    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping("/{id_piso}")
    public ResponseEntity<List<ActivoResponseDTO>> getActivosByPiso(
            @PathVariable("id_piso") Long pisoId,
            @RequestParam(required = false) String search) {
        
        List<ActivoResponseDTO> response = activoService.findByPiso(pisoId, search)
                .stream()
                .map(ActivoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
    /*
    Endpoint Crear un activo en base a un id_piso
    Estado: Funcional
     */
    @PreAuthorize("hasAuthority('PROY_EDITAR')")
    @PostMapping("/{id}/pisos")
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
    /*
    Endpoint Actualizar un activo
    Estado: Funcional
     */
    @PreAuthorize("hasAuthority('PROY_EDITAR')")
    @PutMapping("/{id}")
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
    /*
    Endpoint Eliminar un activo
    Estado: Funcional
     */
    @PreAuthorize("hasAuthority('PROY_EDITAR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarActivo(@PathVariable UUID id) {
        activoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /*
    Endpoint Obtiene los hitosPisos de un piso pero por activo. Mapeado mas general
    Estado: Funcional
     */
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @GetMapping("/{id}/hitos")
    public ResponseEntity<List<HitoPisoResponseDTO>> getActivos(@PathVariable UUID id) {
        List<HitoPiso> hitoPisoes = hitoPisoService.findByActivo(id);
        List<HitoPisoResponseDTO> response = hitoPisoes.stream().map(
                HitoPisoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    /*
    Endpoint Obtiene los hitos de un activo pero con porcentaje obtiene el orden
    Estado: Funcional
     */
    @PreAuthorize("hasAuthority('OBRA_VER')")
    @GetMapping("/{uuid_activo}/avances")
    public ResponseEntity<List<AvanceUnidadResponsePorcentajeDTO>> getAvances(
            @PathVariable("uuid_activo") UUID id) {
        return ResponseEntity.ok(
                hitoPisoService.obtenerAvancesPorActivo(id)
        );
    }

    /*
    Endpoint Obtiene los activos por proyecto_id, page, y size, además de por estado
    Estado: Funcional
     */
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

    /*
    Endpoint Obtiene com ostepper el seguieinto de un piso (buscando el psio en base al uuidActivo)
    Estado: Funcional
     */
    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping("/{uuidActivo}/seguimiento")
    public ResponseEntity<SeguimientoResponseDTO> obtenerSeguimientoObra(@PathVariable UUID uuidActivo) {
        SeguimientoResponseDTO response = seguimientoService.obtenerSeguimiento(uuidActivo);
        return ResponseEntity.ok(response);
    }
}
