package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.request.UpdateContratoDTO;
import com.llosa.backend.proyecto.dto.response.MisActivosResponseDTO;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.dto.request.CrearContratoDTO;
import com.llosa.backend.proyecto.dto.response.UsuarioActivoResponseDTO;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.service.UsuarioService;
import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.service.UsuarioActivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expedientes")
public class UsuarioActivoController {

    private final UsuarioActivoService usuarioActivoService;
    private final UsuarioService usuarioService;

    /**
     * Retorna los activos del cliente autenticado.
     * Navega la lista de copropietarios para filtrar los procesos pertenecientes al usuario logueado.
     * Estado: FUNCIONAL
     */
    @GetMapping("/mis-activos")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<List<MisActivosResponseDTO>> obtenerMisActivos() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String firebaseUuid = authentication.getName();
        Usuario usuarioLogueado = usuarioService.findByFirebaseUuid(firebaseUuid);
        Integer idUsuario = usuarioLogueado.getId();

        // Recupera todos los contratos donde el usuario es propietario o copropietario
        List<UsuarioActivo> misExpedientes = usuarioActivoService.findByUsuario(idUsuario);

        List<MisActivosResponseDTO> activos = misExpedientes.stream()
                .flatMap(expediente -> expediente.getActivos().stream())
                .map(MisActivosResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(activos);
    }

    @GetMapping
    public ResponseEntity<Page<UsuarioActivoResponseDTO>> obtenerTodos(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<UsuarioActivoResponseDTO> paginado = usuarioActivoService.listar(pageable);
        return ResponseEntity.ok(paginado);
    }

    /**
     * Crea un contrato con los ids de los Usuarios. EL activo por ahora queda en null
     * Estado: FUNCIONAL
     */

    @PostMapping("/crear")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<UsuarioActivoResponseDTO> crearContratoBase(@Valid @RequestBody CrearContratoDTO dto) {
        UsuarioActivo usuarioActivo = usuarioActivoService.crearContratoBase(dto);
        UsuarioActivoResponseDTO response = UsuarioActivoResponseDTO.fromEntity(usuarioActivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    /**
     * Update un contrato
     * Estado: FUNCIONAL
     */

    @PutMapping("/{uuidExpediente}")
    @PreAuthorize("hasAuthority('CONTRAT_EDITAR')")
    public ResponseEntity<UsuarioActivoResponseDTO> actualizarContrato(@PathVariable UUID uuidExpediente, @Valid @RequestBody UpdateContratoDTO dto) {
        UsuarioActivo usuarioActivo = usuarioActivoService.actualizarCompleto(uuidExpediente,dto);
        UsuarioActivoResponseDTO response = UsuarioActivoResponseDTO.fromEntity(usuarioActivo);
        return ResponseEntity.ok(response);
    }

    /**
    Endpoint Asignar un activo a un contrato
    Estado: Funcional Hecho
     */
    @PostMapping("/asignar")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<UsuarioActivoResponseDTO> asignarActivosAContrato(@Valid @RequestBody AsignarActivoDTO dto) {
        UsuarioActivoResponseDTO usuarioActivoResponseDTO = usuarioActivoService.asignarActivo(dto);
        return ResponseEntity.ok().body(usuarioActivoResponseDTO);
    }
  //________________- Aqui me quede

    /**
     * Retorna el contrato/proceso comercial de un activo con la lista completa de copropietarios.
     * Estado: FUNCIONAL
     */
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @GetMapping("/contrato/{uuidExpediente}")
    public ResponseEntity<UsuarioActivoResponseDTO> obtenerContratoPorId(@PathVariable UUID uuidExpediente) {
        return ResponseEntity.ok(UsuarioActivoResponseDTO.fromEntity(usuarioActivoService.findById(uuidExpediente)));
    }

    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @GetMapping("/{uuidActivo}/contrato")
    public ResponseEntity<UsuarioActivoResponseDTO> verContratoActivoUsuario(@PathVariable UUID uuidActivo) {
        return usuarioActivoService.findByActivo(uuidActivo)
                .map(UsuarioActivoResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retorna contrato de un usario
     * Estado: funcional
     */
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @GetMapping("/{idUsuario}")
    public ResponseEntity<List<UsuarioActivoResponseDTO>> obtenerActivosPorUsuario(@PathVariable Integer idUsuario) {
        List<UsuarioActivo> activos = usuarioActivoService.findByUsuario(idUsuario);
        List<UsuarioActivoResponseDTO> response = activos.stream()
                .map(UsuarioActivoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna la lista de activos asociados a un usuario específico.
     * Estado: FUncional -> Posible probelma de N+1
     */
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @GetMapping("/usuario/{idUsuario}/activos")
    public ResponseEntity<List<ActivoResponseDTO>> obtenerSoloActivosPorUsuario(@PathVariable Integer idUsuario) {
        return ResponseEntity.ok(
                usuarioActivoService.findByUsuarioId(idUsuario)
                        .stream()
                        .map(ActivoResponseDTO::fromEntity)
                        .toList()
        );
    }

    /**
     * Retorna el usuario eliminado
     * Estado: Funcional
     */
    @DeleteMapping("/delete/{uuidUsuarioActivo}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<Void> eliminarContrato(@PathVariable UUID uuidUsuarioActivo) {
        usuarioActivoService.eliminarContrato(uuidUsuarioActivo);
        return ResponseEntity.noContent().build();
    }

    /** Asinar un asesor**/

    @PostMapping("/usuarioActivo/{idUsuarioActivo}/asesor/{idAsesor}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<UsuarioActivoResponseDTO> asignarUnAsesorAlContrato(
            @PathVariable UUID idUsuarioActivo,
            @PathVariable Integer idAsesor){
        UsuarioActivo usuario = usuarioActivoService.asignarAsesorAContrato(idUsuarioActivo, idAsesor);
        return ResponseEntity.ok().body(UsuarioActivoResponseDTO.fromEntity(usuario));
    }

    @PutMapping("/usuarioActivo/{idUsuarioActivo}/asesor/{idAsesor}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<UsuarioActivoResponseDTO> desvincularAsesorDelContrato(
            @PathVariable UUID idUsuarioActivo,
            @PathVariable Integer idAsesor){
        UsuarioActivo usuario = usuarioActivoService.desasignarAsesorDelContrato(idUsuarioActivo, idAsesor);
        return ResponseEntity.ok().body(UsuarioActivoResponseDTO.fromEntity(usuario));
    }


}
