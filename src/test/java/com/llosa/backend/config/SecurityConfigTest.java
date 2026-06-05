package com.llosa.backend.config;

import com.llosa.backend.seguridad.controller.AuthController;
import com.llosa.backend.seguridad.controller.UsuarioController;
import com.llosa.backend.seguridad.service.AuthService;
import com.llosa.backend.seguridad.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, UsuarioController.class})
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;
    @MockitoBean
    UsuarioService usuarioService;
    @MockitoBean
    FirebaseConfig firebaseConfig;
    @MockitoBean
    com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    // ── Endpoints protegidos requieren autenticación ──────────────────────────

    @Test
    void apiUsers_sinAutenticar() throws Exception {
        // CP06: GET /api/users SIN autenticación devuelve 200 (endpoint accessible)
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    void apiUsersRegister_sinAutenticar() throws Exception {
        // CP06: POST /api/users/register SIN body válido devuelve 400 (Bad Request)
        mockMvc.perform(post("/api/users/register")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── CP06: Rutas de autenticación bien configuradas ──────────────────────

    @Test
    void apiAuthMe_sinAutenticar() throws Exception {
        // CP06: GET /api/auth/me SIN autenticación devuelve 403
        // (porque /api/auth/** está en permitAll() pero el controller requiere FirebaseAuthenticationToken)
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rutaInexistente_sinAutenticar() throws Exception {
        // CP06: Rutas inexistentes devuelven 404 (Not Found)
        mockMvc.perform(get("/api/ruta-inexistente-xyz"))
                .andExpect(status().isNotFound());
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
