package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.ProyectoCargaDTO;
import com.llosa.backend.proyecto.dto.request.ProyectoCreateDTO;
import com.llosa.backend.proyecto.dto.response.DashboardProyectoDTO;
import com.llosa.backend.proyecto.dto.response.ProyectoResponseDTO;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.service.ProyectoService;
import com.llosa.backend.proyecto.dto.request.HitoCreateDTO;
import com.llosa.backend.proyecto.dto.response.HitoResponseDTO;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.service.HitoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/proyectos")
@RequiredArgsConstructor
public class ProyectoController {

    private final ProyectoService proyectoService;
    private final HitoService hitoService;

    // Funcionando correctamente
    @PreAuthorize("hasAuthority('PROY_CREAR')")
    @PostMapping
    public ResponseEntity<ProyectoResponseDTO> crearProyecto(@Valid @RequestBody ProyectoCreateDTO proyecto) {
        Proyecto nuevo_proyecto = Proyecto.builder()
                .nombre(proyecto.nombre())
                .descripcion(proyecto.descripcion())
                .precertificacionEdgeLeed(proyecto.precertificacionEdgeLeed())
                .linkRecorridoVirtual(proyecto.linkRecorridoVirtual())
                .departamento(proyecto.departamento())
                .distrito(proyecto.distrito())
                .direccion(proyecto.direccion())
                .fechaInicio(proyecto.fechaInicio())
                .fechaFin(proyecto.fechaFin())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ProyectoResponseDTO.fromEntity(proyectoService.save(nuevo_proyecto)));
    }

    @PreAuthorize("hasAuthority('PROY_EDITAR')")
    @PutMapping("/{uuid}")
    public ResponseEntity<ProyectoResponseDTO> actualizarProyecto(@PathVariable("uuid") UUID id, @Valid @RequestBody ProyectoCreateDTO proyecto) {
        Proyecto proyectoActualizado = proyectoService.findById(id);
        proyectoActualizado.setNombre(proyecto.nombre());
        proyectoActualizado.setDescripcion(proyecto.descripcion());
        proyectoActualizado.setPrecertificacionEdgeLeed(proyecto.precertificacionEdgeLeed());
        proyectoActualizado.setLinkRecorridoVirtual(proyecto.linkRecorridoVirtual());
        proyectoActualizado.setDepartamento(proyecto.departamento());
        proyectoActualizado.setDistrito(proyecto.distrito());
        proyectoActualizado.setDireccion(proyecto.direccion());
        proyectoActualizado.setFechaInicio(proyecto.fechaInicio());
        proyectoActualizado.setFechaFin(proyecto.fechaFin());
        return ResponseEntity.ok(ProyectoResponseDTO.fromEntity(proyectoService.save(proyectoActualizado)));
    }

    @PreAuthorize("hasAuthority('PROY_EDITAR')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteProyecto(@PathVariable("uuid") UUID id) {
        proyectoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Funcionando correctamente
    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping
    public ResponseEntity<List<ProyectoResponseDTO>> findAll() {
        List<ProyectoResponseDTO> response = proyectoService.findAll()
                .stream()
                .map(ProyectoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
    // Funcionando correctamente
    // Funcionando correctamente
    @PreAuthorize("hasAuthority('PROY_EDITAR')")
    @PostMapping("/{uuid}/hitos")
    public ResponseEntity<HitoResponseDTO> crearHito(@PathVariable("uuid") UUID id_proyecto,
                                                       @Valid @RequestBody HitoCreateDTO dto) {
        Hito hito = Hito.builder()
                .titulo(dto.titulo())
                .orden(dto.orden())
                .tipo(dto.tipo())
                .estado(EstadoHito.PENDIENTE)
                .build();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(HitoResponseDTO.fromEntity(hitoService.save(id_proyecto, hito)));
    }
    // Super endopint para la creacion de torres , pisos y activos
    @PreAuthorize("hasAuthority('PROY_EDITAR')")
    @PostMapping("{id_proyecto}/estructura-fisica")
    public ResponseEntity<Void> crearEstructuraFisica(@PathVariable("id_proyecto") UUID id_proyecto,
                                                      @Valid @RequestBody ProyectoCargaDTO estructuraFisica) {
        proyectoService.cargarProyecto(id_proyecto,estructuraFisica);
        return ResponseEntity.ok().build();
    }

    // Funciona correctamente
    // Genera el porcentaje total de avance de un proyecto por sus hitos
    @PreAuthorize("hasAuthority('PROY_VER')")
    @GetMapping("/{uuid}/avance-general")
    public ResponseEntity<DashboardProyectoDTO> getAvanceGeneral(@PathVariable("uuid") UUID id) {
        Proyecto proyecto = proyectoService.findById(id);
        double avance = proyectoService.getPorcentajeAvance(id);
        return ResponseEntity.ok(new DashboardProyectoDTO(proyecto.getId(), proyecto.getNombre(), avance));
    }
}
