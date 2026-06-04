package com.llosa.backend.seguridad.controller;

import com.llosa.backend.seguridad.dto.PerfilConPermisosResponse;
import com.llosa.backend.seguridad.service.AuthService;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<PerfilConPermisosResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication instanceof FirebaseAuthenticationToken)) {
            return ResponseEntity.status(403).build();
        }

        FirebaseAuthenticationToken auth = (FirebaseAuthenticationToken) authentication;
        PerfilConPermisosResponse perfil = authService.verificarYCargarPerfil(auth.getUid(), auth.getEmail());
        return ResponseEntity.ok(perfil);
    }
}

