package com.llosa.backend.module.seguridad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.TestData;
import com.llosa.backend.module.seguridad.dto.AsignarRolRequest;
import com.llosa.backend.module.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.module.seguridad.dto.UsuarioResponse;
import com.llosa.backend.module.seguridad.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UsuarioService usuarioService;

    @MockBean
    FirebaseConfig firebaseConfig;

    // ── POST /api/users/register ─────────────────────────────────────────────

    @Test
    void register_sinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.crearUsuarioRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_bodyInvalidoSinEmail_devuelve400() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setEmail(null);

        mockMvc.perform(post("/api/users/register")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_bodyInvalidoSinNombre_devuelve400() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setNombre("");

        mockMvc.perform(post("/api/users/register")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_bodyValido_devuelve200ConRespuesta() throws Exception {
        UsuarioResponse resp = new UsuarioResponse();
        resp.setId(1);
        resp.setEmail("ana.garcia@test.com");
        resp.setNombre("Ana");
        resp.setTipoUsuario("CLIENTE");
        resp.setActivo(true);
        resp.setFunciones(List.of());

        when(usuarioService.crearUsuario(any())).thenReturn(resp);

        mockMvc.perform(post("/api/users/register")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestData.crearUsuarioRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana.garcia@test.com"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    // ── GET /api/users ────────────────────────────────────────────────────────

    @Test
    void listar_devuelveListaDeUsuarios() throws Exception {
        UsuarioResponse u = new UsuarioResponse();
        u.setId(1);
        u.setEmail("user@test.com");
        u.setNombre("User");
        u.setActivo(true);
        u.setFunciones(List.of());

        when(usuarioService.listarTodos()).thenReturn(List.of(u));

        mockMvc.perform(get("/api/users")
                        .with(authentication(TestData.authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("user@test.com"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ── PUT /api/users/{id}/role ─────────────────────────────────────────────

    @Test
    void asignarRol_sinIdRol_devuelve400() throws Exception {
        AsignarRolRequest req = new AsignarRolRequest();
        // idRol es null → @NotNull falla

        mockMvc.perform(put("/api/users/1/role")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void asignarRol_valido_devuelve200() throws Exception {
        AsignarRolRequest req = new AsignarRolRequest();
        req.setIdRol(2);

        UsuarioResponse resp = new UsuarioResponse();
        resp.setId(1);
        resp.setRol("ASESOR");
        resp.setFunciones(List.of("PROY_VER"));
        resp.setActivo(true);

        when(usuarioService.asignarRol(eq(1), eq(2))).thenReturn(resp);

        mockMvc.perform(put("/api/users/1/role")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ASESOR"));
    }

    // ── DELETE /api/users/{id} ────────────────────────────────────────────────

    @Test
    void desactivar_devuelve200() throws Exception {
        doNothing().when(usuarioService).cambiarEstado(eq(5), eq(false));

        mockMvc.perform(delete("/api/users/5")
                        .with(authentication(TestData.authToken()))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(usuarioService).cambiarEstado(5, false);
    }

    @Test
    void desactivar_sinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(delete("/api/users/5").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
