package com.llosa.backend.seguridad.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(7);
        log.warn("Token recibido (primeros 50 chars): {}", idToken.substring(0, Math.min(50, idToken.length())));

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid   = decoded.getUid();
            String email = decoded.getEmail();

            // 1. Buscamos al usuario usando el método de tu UsuarioRepository
            Usuario usuario = usuarioRepository.findByFirebaseUuid(uid)
                    .orElseThrow(() -> new RuntimeException("Usuario verificado en Firebase pero no existe en BD"));

            // 2. Leemos sus funciones de la BD de forma segura
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            if (usuario.getRol() != null && usuario.getRol().getFunciones() != null) {
                authorities = usuario.getRol().getFunciones().stream()
                        // 3. Usamos tu getNombreCodigo() de la entidad Funcion
                        .map(funcion -> new SimpleGrantedAuthority(funcion.getNombreCodigo()))
                        .collect(Collectors.toList());
            }

            // 4. Inyectamos la lista de funciones (authorities) a Spring Security
            FirebaseAuthenticationToken authentication =
                    new FirebaseAuthenticationToken(uid, email, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.warn("Token Firebase inválido o error de BD: {}", e.getMessage());
            log.warn("Causa: {}", e.getClass().getName());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}