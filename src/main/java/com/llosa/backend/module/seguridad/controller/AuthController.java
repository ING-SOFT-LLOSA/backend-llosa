package com.llosa.backend.module.seguridad.controller;

import com.llosa.backend.module.seguridad.dto.PerfilConPermisosResponse;
import com.llosa.backend.module.seguridad.service.AuthService;
import com.llosa.backend.security.FirebaseAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<PerfilConPermisosResponse> me(
            @AuthenticationPrincipal FirebaseAuthenticationToken auth) {
        PerfilConPermisosResponse perfil =
                authService.verificarYCargarPerfil(auth.getUid(), auth.getEmail());
        return ResponseEntity.ok(perfil);
    }
}