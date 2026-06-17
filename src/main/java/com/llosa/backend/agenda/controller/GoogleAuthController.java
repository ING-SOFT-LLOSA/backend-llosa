package com.llosa.backend.agenda.controller;

import com.llosa.backend.agenda.dto.response.GoogleAuthUrlResponse;
import com.llosa.backend.agenda.service.GoogleOAuthService;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleOAuthService googleOAuthService;
    private final UsuarioRepository usuarioRepository;

    /**
     * El frontend llama a este endpoint (autenticado) para obtener el link
     * de autorización de Google y redirigir/abrir una ventana con él.
     *
     * NOTA: el principal del Authentication es el firebaseUuid (String),
     * no un objeto Usuario (ver FirebaseAuthenticationToken / FirebaseTokenFilter).
     * Por eso resolvemos el Usuario real desde el repositorio.
     */
    @GetMapping("/url")
    public ResponseEntity<GoogleAuthUrlResponse> obtenerUrlAutorizacion(Authentication authentication) {
        String firebaseUuid = authentication.getName();

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUuid)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado para UID: " + firebaseUuid));

        String url = googleOAuthService.generarUrlAutorizacion(usuario.getId());
        return ResponseEntity.ok(new GoogleAuthUrlResponse(url));
    }

    /**
     * Google redirige aquí después de que el gestor acepta (o rechaza) los permisos.
     * Este endpoint NO lo llama el frontend directamente, lo llama Google.
     * Es público (ver SecurityConfig) porque Google no envía un token Firebase.
     */
    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        if (error != null) {
            return ResponseEntity.ok("Autorización cancelada: " + error
                    + ". Puedes cerrar esta ventana e intentarlo de nuevo desde la app.");
        }

        googleOAuthService.procesarCallback(code, state);

        return ResponseEntity.ok("Tu Google Calendar fue conectado exitosamente. " +
                "Ya puedes cerrar esta ventana.");
    }

    /**
     * Permite al gestor desconectar su cuenta de Google Calendar.
     */
    @DeleteMapping
    public ResponseEntity<Void> desconectar(Authentication authentication) {
        String firebaseUuid = authentication.getName();

        Usuario usuario = usuarioRepository.findByFirebaseUuid(firebaseUuid)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado para UID: " + firebaseUuid));

        googleOAuthService.desconectar(usuario.getId());
        return ResponseEntity.noContent().build();
    }
}