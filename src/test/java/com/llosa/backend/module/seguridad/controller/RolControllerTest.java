package com.llosa.backend.module.seguridad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.module.seguridad.dto.ModificarFuncionesRequest;
import com.llosa.backend.module.seguridad.entity.Rol;
import com.llosa.backend.module.seguridad.service.RolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
class RolControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RolService rolService;

    @MockitoBean
    FirebaseConfig firebaseConfig;

    // ── GET /api/roles ────────────────────────────────────────────────────────

    @Test
    void listar_sinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listar_devuelveTodosLosRoles() throws Exception {
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
        when(rolService.listarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/api/roles")
                        .with(authentication(TestData.authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── PUT /api/roles/{id}/functions ─────────────────────────────────────────

    @Test
    void modificarFunciones_sinIdFunciones_devuelve400() throws Exception {
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
    void modificarFunciones_valido_devuelve200ConRolActualizado() throws Exception {
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
    void modificarFunciones_listaVacia_devuelve200() throws Exception {
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
    void modificarFunciones_sinAutenticar_devuelve401() throws Exception {
        ModificarFuncionesRequest req = new ModificarFuncionesRequest();
        req.setIdFunciones(List.of(1));

        mockMvc.perform(put("/api/roles/1/functions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void modificarFunciones_sinContentType_devuelve415() throws Exception {
        mockMvc.perform(put("/api/roles/1/functions")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .content("{\"idFunciones\":[1,2]}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void modificarFunciones_rolNoEncontrado_devuelve404() throws Exception {
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
