package com.llosa.backend.seguridad.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseTokenFilter extends OncePerRequestFilter {

    // Inyectamos tu repositorio exacto
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(7);
        log.info("Procesando intento de autenticación con token Firebase.");

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid   = decoded.getUid();
            String email = decoded.getEmail();

            Usuario usuario = usuarioRepository.findByFirebaseUuid(uid)
                    .orElseThrow(() -> new BadCredentialsException("Usuario verificado en Firebase pero no existe en BD"));

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            if (usuario.getRol() != null && usuario.getRol().getFunciones() != null) {
                authorities = usuario.getRol().getFunciones().stream()
                        .map(funcion -> new SimpleGrantedAuthority(funcion.getNombreCodigo()))
                        .collect(Collectors.toList());
            }

            FirebaseAuthenticationToken authentication =
                    new FirebaseAuthenticationToken(uid, email, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (FirebaseAuthException | BadCredentialsException e) {
            // CASO 1: Fallas de autenticación (Token malo, expirado, o usuario no existe en BD)
            log.warn("Autenticación rechazada - Credenciales inválidas: {}", e.getMessage());
            SecurityContextHolder.clearContext();

            // Le indicamos a la respuesta que no está autorizado
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            // NOTA: No llamamos a filterChain.doFilter para cortar la petición de forma segura aquí.

        } catch (Exception e) {
            // CASO 2: Errores imprevistos reales del sistema (Bugs, caída de BD, NullPointer)
            log.error("ERROR CRÍTICO EN FILTRO DE AUTENTICACIÓN: Un error inesperado ocurrió en el servidor", e);
            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"Internal server error durante la autenticación.\"}");
        }
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html");
    }
}