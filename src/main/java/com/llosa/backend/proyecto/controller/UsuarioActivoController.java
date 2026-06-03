package com.llosa.backend.proyecto.controller;

import com.llosa.backend.proyecto.dto.response.UsuarioActivoResponseDTO;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.service.UsuarioService;
import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.UsuarioActivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final ActivoService activoService;
    private final UsuarioService usuarioService;

    /**
     * Retorna los activos del cliente autenticado.
     * Navega la lista de copropietarios para filtrar los procesos pertenecientes al usuario logueado.
     */
    @GetMapping("/mis-activos")
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    public ResponseEntity<List<ActivoResponseDTO>> obtenerMisActivos() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String firebaseUuid = authentication.getName();
        Usuario usuarioLogueado = usuarioService.findByFirebaseUuid(firebaseUuid);
        Integer idUsuario = usuarioLogueado.getId();

        // Recupera todos los procesos en los que el usuario es copropietario
        List<UsuarioActivo> misExpedientes = usuarioActivoService.findByUsuario(idUsuario);
        List<ActivoResponseDTO> activos = misExpedientes.stream()
                .map(expediente -> ActivoResponseDTO.fromEntity(expediente.getActivo()))
                .toList();

        return ResponseEntity.ok(activos);
    }

    @PostMapping("/asignar")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<String> asignarActivoAUsuario(@Valid @RequestBody AsignarActivoDTO dto) {
        usuarioActivoService.asignarActivo(dto);
        return ResponseEntity.ok("Activo asignación registrada con éxito.");
    }

    /**
     * Retorna el contrato/proceso comercial de un activo con la lista completa de copropietarios.
     */
    @PreAuthorize("hasAuthority('CONTRATO_VER')")
    @GetMapping("/{uuidActivo}/contrato")
    public ResponseEntity<UsuarioActivoResponseDTO> verContratoActivoUsuario(@PathVariable UUID uuidActivo) {
        return usuarioActivoService.findByActivo(uuidActivo)
                .map(UsuarioActivoResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete/{uuidUsuarioActivo}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<Void> desvicularActivoAUsuario(@PathVariable UUID uuidUsuarioActivo) {
        usuarioActivoService.deleteById(uuidUsuarioActivo);
        return ResponseEntity.noContent().build();
    }
}
