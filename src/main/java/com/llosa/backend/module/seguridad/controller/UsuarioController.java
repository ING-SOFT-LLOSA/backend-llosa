package com.llosa.backend.module.seguridad.controller;

import com.llosa.backend.module.seguridad.dto.AsignarRolRequest;
import com.llosa.backend.module.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.module.seguridad.dto.UsuarioResponse;
import com.llosa.backend.module.seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest req)
            throws Exception {
        return ResponseEntity.ok(usuarioService.crearUsuario(req));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UsuarioResponse> asignarRol(@PathVariable Integer id,
                                                      @Valid @RequestBody AsignarRolRequest req) {
        return ResponseEntity.ok(usuarioService.asignarRol(id, req.getIdRol()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) throws Exception {
        usuarioService.cambiarEstado(id, false);
        return ResponseEntity.ok().build();
    }

    // Endpoint temporal solo para desarrollo - eliminar completamente un usuario
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> eliminarCompletamente(@PathVariable Integer id) throws Exception {
        usuarioService.eliminarCompletamente(id);
        return ResponseEntity.ok().build();
    }

    // Paginado correo corectamente
    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('USER_GESTIONAR')") // Aqui debería ser user ver
    public ResponseEntity<Page<UsuarioResponseFunciones>> listar_paginado(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UsuarioResponseFunciones> resultado = usuarioService.listarPaginadoYFiltrado(search, page, size);
        return ResponseEntity.ok(resultado);
    }
}