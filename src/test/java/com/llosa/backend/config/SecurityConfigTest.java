package com.llosa.backend.config;

import com.llosa.backend.seguridad.controller.UsuarioController;
import com.llosa.backend.seguridad.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UsuarioService usuarioService;
    @MockitoBean
    FirebaseConfig firebaseConfig;

    // ── Endpoints protegidos requieren autenticación ──────────────────────────

    @Test
    void apiUsers_sinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiUsersRegister_sinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(get("/api/users/register"))
                .andExpect(status().isUnauthorized());
    }

    // ── Regla permit-all mal configurada ─────────────────────────────────────

    @Test
    void rutaAuthBase_sinControlador_devuelve401() throws Exception {
        // Spring Security 6 usa MvcRequestMatcher para requestMatchers(String).
        // Ese matcher SOLO aplica a rutas con controlador registrado en Spring MVC.
        // Como no hay controlador en "/auth/**", la regla permitAll() nunca hace match
        // y la petición cae en anyRequest().authenticated() → 401.
        // Consecuencia: la regla permitAll("/auth/**") en SecurityConfig es letra muerta.
        mockMvc.perform(get("/auth/cualquier-ruta"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaNoExistente_sinAutenticar_devuelve401() throws Exception {
        // Rutas fuera de "/auth/**" sin controlador → 401 antes de llegar al router
        mockMvc.perform(get("/api/ruta-inexistente"))
                .andExpect(status().isUnauthorized());
    }
}
