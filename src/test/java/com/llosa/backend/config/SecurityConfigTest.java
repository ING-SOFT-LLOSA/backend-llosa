package com.llosa.backend.config;

import com.llosa.backend.module.seguridad.controller.UsuarioController;
import com.llosa.backend.module.seguridad.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void apiUsers_sinAutenticar() throws Exception {
        // CP06: GET /api/users SIN autenticación DEBE devolver 401
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiUsersRegister_sinAutenticar() throws Exception {
        // CP06: POST /api/users/register SIN autenticación DEBE devolver 401
        mockMvc.perform(post("/api/users/register")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── CP06: Rutas de autenticación bien configuradas ──────────────────────

    @Test
    void apiAuthMe_sinAutenticar() throws Exception {
        // CP06: GET /api/auth/me SIN autenticación DEBE devolver 401
        // (NO 403, que indica error de servidor)
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaInexistente_sinAutenticar() throws Exception {
        // CP06: Rutas inexistentes sin autenticación deben devolver 401
        // ANTES de devolver 404
        mockMvc.perform(get("/api/ruta-inexistente-xyz"))
                .andExpect(status().isUnauthorized());
    }

    // ── Validación de permitAll() para rutas de autenticación ───────────────────

    @Test
    void apiAuthRuta_sinAutenticar_permitida() throws Exception {
        // CP06: Rutas bajo /api/auth/** DEBEN estar permitidas sin autenticación
        // Aunque no haya controlador, NO debería devolver 401 (porque está en permitAll())
        // Debería devolver 404 (ruta no existe) o 405 (método no permitido)
        // PERO NO 401 (que indicaría que se requiere autenticación)
        mockMvc.perform(get("/api/auth/ruta-inexistente"))
                .andExpect(status().isNotFound());  // 404, no 401
    }
}
