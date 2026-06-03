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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expedientes")
public class UsuarioActivoController {

    private final UsuarioActivoService usuarioActivoService;
    private final ActivoService activoService;
    private final UsuarioService usuarioService;

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
        List<UsuarioActivo> misExpedientes = usuarioActivoService.findByUsuario(idUsuario);
        List<ActivoResponseDTO> activos = misExpedientes.stream().map(
                expediente -> ActivoResponseDTO.fromEntity(expediente.getActivo())
        ).toList();
        return ResponseEntity.ok(activos);
    }

    @PostMapping("/asignar")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')") // Solo personal autorizado
    public ResponseEntity<String> asignarActivoAUsuario(@Valid @RequestBody AsignarActivoDTO dto) {
        usuarioActivoService.asignarActivo(dto);
        return ResponseEntity.ok("Activo asignación registrada con éxito.");
    }

    // ver el contrato con el usuario y activo
    @GetMapping("/{uuidActivo}/contrato")
    public ResponseEntity<UsuarioActivoResponseDTO> verContratoActivoUsuario(@PathVariable UUID uuidActivo) {
        return usuarioActivoService.findByActivo(uuidActivo)
                .map(UsuarioActivoResponseDTO::fromEntity) // Si existe, mapea a DTO
                .map(ResponseEntity::ok)                   // Y mete el DTO en un 200 OK
                .orElseGet(() -> ResponseEntity.notFound().build()); // Si no, 404
    }

    // Borrar el contrato con el usuario y activo
    @DeleteMapping("/delete/{uuidUsuarioActivo}")
    @PreAuthorize("hasAuthority('CONTRATO_EDITAR')")
    public ResponseEntity<Void> desvicularActivoAUsuario(@PathVariable UUID uuidUsuarioActivo) {
        usuarioActivoService.deleteById(uuidUsuarioActivo);
        return ResponseEntity.noContent().build();
    }
}
