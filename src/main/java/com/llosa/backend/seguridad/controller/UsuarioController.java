package com.llosa.backend.seguridad.controller;

import com.llosa.backend.seguridad.dto.*;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PreAuthorize("hasAuthority('USER_GESTIONAR')")
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest req)
            throws Exception {
        return ResponseEntity.ok(usuarioService.crearUsuario(req));
    }

    @PreAuthorize("hasAuthority('USER_VER')")
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @PreAuthorize("hasAuthority('USER_GESTIONAR')")
    @PutMapping("/{id}/role")
    public ResponseEntity<UsuarioResponse> asignarRol(@PathVariable Integer id,
                                                      @Valid @RequestBody AsignarRolRequest req) {
        return ResponseEntity.ok(usuarioService.asignarRol(id, req.getIdRol()));
    }

    @PreAuthorize("hasAuthority('USER_GESTIONAR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) throws Exception {
        usuarioService.cambiarEstado(id, false);
        return ResponseEntity.ok().build();
    }

    // Endpoint temporal solo para desarrollo - eliminar completamente un usuario
    @PreAuthorize("hasAuthority('USER_GESTIONAR')")
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> eliminarCompletamente(@PathVariable Integer id) throws Exception {
        usuarioService.eliminarCompletamente(id);
        return ResponseEntity.ok().build();
    }

    // Paginado correo corectamente
    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('USER_GESTIONAR')")
    public ResponseEntity<Page<UsuarioResponseFunciones>> listar_paginado(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UsuarioResponseFunciones> resultado = usuarioService.listarPaginadoYFiltrado(search, page, size);
        return ResponseEntity.ok(resultado);
    }

    // Buscar un usario por id.
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_GESTIONAR')")
    public ResponseEntity<UsuarioResponse> buscarUsuarioPorId(@PathVariable Integer id) {

        Usuario usuario = usuarioService.findById(id);

        return ResponseEntity.ok(usuarioService.toResponse(usuario));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_GESTIONAR')")
    public ResponseEntity<UsuarioResponse> modificarInformacionUsuario(@PathVariable Integer id, @RequestBody UpdateUsuarioDTO updateUsuarioDTO){
        UsuarioResponse usuarioResponse = usuarioService.actualizarUsuario(id, updateUsuarioDTO);
        return ResponseEntity.ok(usuarioResponse);
    }
}