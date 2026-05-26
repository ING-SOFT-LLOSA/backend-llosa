package com.llosa.backend.proyecto.controller;

import com.llosa.backend.module.seguridad.service.UsuarioService;
import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.entity.UsuarioActivo;

import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.UsuarioActivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        String emailUsuarioAutenticado = authentication.getName();
        List<UsuarioActivo> misExpedientes = usuarioActivoService.findByUsuarioEmail(emailUsuarioAutenticado);
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

}
