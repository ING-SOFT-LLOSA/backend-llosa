package com.llosa.backend.module.seguridad.controller;

import com.llosa.backend.module.seguridad.dto.ModificarFuncionesRequest;
import com.llosa.backend.module.seguridad.entity.Rol;
import com.llosa.backend.module.seguridad.service.RolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @GetMapping
    public ResponseEntity<List<Rol>> listar() {
        return ResponseEntity.ok(rolService.listarTodos());
    }

    @PutMapping("/{id}/functions")
    public ResponseEntity<Rol> modificarFunciones(@PathVariable Integer id,
                                                  @Valid @RequestBody ModificarFuncionesRequest req) {
        return ResponseEntity.ok(rolService.modificarFunciones(id, req.getIdFunciones()));
    }
}