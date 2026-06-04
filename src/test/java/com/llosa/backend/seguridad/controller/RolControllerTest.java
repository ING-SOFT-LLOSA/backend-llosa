package com.llosa.backend.seguridad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.dto.ModificarFuncionesRequest;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.service.RolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RolController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class})
class RolControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RolService rolService;

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @MockitoBean
    com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    // ── GET /api/roles ────────────────────────────────────────────────────────

    @Test
    void listar_sinAutenticar_devuelve401() throws Exception {
        // CP06/CP07: GET /api/roles sin token DEBE devolver 401 (NO 200)
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listar_devuelveTodosLosRoles() throws Exception {
        // Devuelve 200 con la lista de roles
        Rol rol = TestData.rol();
        when(rolService.listarTodos()).thenReturn(List.of(rol));

        mockMvc.perform(get("/api/roles")
                        .with(authentication(TestData.authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("CLIENTE"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listar_conListaVacia_devuelveArrayVacio() throws Exception {
        // Devuelve 200 con array vacío
        when(rolService.listarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/api/roles")
                        .with(authentication(TestData.authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── PUT /api/roles/{id}/functions ─────────────────────────────────────────

    @Test
    void modificarFunciones_sinIdFunciones() throws Exception {
        // Devuelve 400
        ModificarFuncionesRequest req = new ModificarFuncionesRequest();
        // idFunciones es null → @NotNull falla

        mockMvc.perform(put("/api/roles/1/functions")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modificarFunciones_valido() throws Exception {
        // Devuelve 200
        ModificarFuncionesRequest req = new ModificarFuncionesRequest();
        req.setIdFunciones(List.of(1, 2, 3));

        Rol rolActualizado = TestData.rol();
        rolActualizado.setFunciones(List.of(
                TestData.funcion("PROY_VER"),
                TestData.funcion("DOCS_VER"),
                TestData.funcion("PAGOS_VER")
        ));

        when(rolService.modificarFunciones(eq(1), eq(List.of(1, 2, 3))))
                .thenReturn(rolActualizado);

        mockMvc.perform(put("/api/roles/1/functions")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("CLIENTE"));
    }

    @Test
    void modificarFunciones_listaVacia() throws Exception {
        // Devuelve 200
        ModificarFuncionesRequest req = new ModificarFuncionesRequest();
        req.setIdFunciones(List.of());

        Rol rolSinFunciones = TestData.rol();
        rolSinFunciones.setFunciones(List.of());
        when(rolService.modificarFunciones(eq(2), eq(List.of()))).thenReturn(rolSinFunciones);

        mockMvc.perform(put("/api/roles/2/functions")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void modificarFunciones_sinAutenticar() throws Exception {
        // Devuelve 401
        ModificarFuncionesRequest req = new ModificarFuncionesRequest();
        req.setIdFunciones(List.of(1));

        mockMvc.perform(put("/api/roles/1/functions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void modificarFunciones_sinContentType() throws Exception {
        // Devuelve 415
        mockMvc.perform(put("/api/roles/1/functions")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .content("{\"idFunciones\":[1,2]}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void modificarFunciones_rolNoEncontrado() throws Exception {
        // Devuelve 404
        ModificarFuncionesRequest req = new ModificarFuncionesRequest();
        req.setIdFunciones(List.of(1, 2));
        when(rolService.modificarFunciones(eq(99), any()))
                .thenThrow(new RecursoNoEncontradoException("Rol no encontrado"));

        mockMvc.perform(put("/api/roles/99/functions")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Rol no encontrado"));
    }
}
