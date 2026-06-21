package com.llosa.backend.seguridad.controller;

import com.llosa.backend.seguridad.dto.PerfilConPermisosResponse;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import com.llosa.backend.seguridad.service.AuthService;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/me")
    public ResponseEntity<PerfilConPermisosResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof FirebaseAuthenticationToken auth)) {
            return ResponseEntity.status(403).build();
        }

        PerfilConPermisosResponse perfil = authService.verificarYCargarPerfil(auth.getUid());
        return ResponseEntity.ok(perfil);
    }

    @GetMapping("/email-exists")
    public ResponseEntity<Map<String, Boolean>> emailExists(@RequestParam String email) {
        boolean exists = usuarioRepository.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}