package com.llosa.backend.seguridad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.dto.ModificarFuncionesRequest;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.service.RolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RolControllerTest {

    private MockMvc mockMvc;

    @Mock
    RolService rolService;

    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RolController(rolService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── GET /api/roles ────────────────────────────────────────────────────────

    @Test
    void listar_devuelveTodosLosRoles() throws Exception {
        Rol rol = TestData.rol();
        when(rolService.listarTodos()).thenReturn(List.of(rol));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("CLIENTE"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listar_conListaVacia_devuelveArrayVacio() throws Exception {
        when(rolService.listarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── PUT /api/roles/{id}/functions ─────────────────────────────────────────

    @Test
    void modificarFunciones_sinIdFunciones_devuelve400() throws Exception {
        ModificarFuncionesRequest req = new ModificarFuncionesRequest();

        mockMvc.perform(put("/api/roles/1/functions")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void modificarFunciones_sinContentType_devuelve415() throws Exception {
        ModificarFuncionesRequest req = new ModificarFuncionesRequest();
        req.setIdFunciones(List.of(1));

        mockMvc.perform(put("/api/roles/1/functions")
                        .header("Content-Type", "text/plain")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Rol no encontrado"));
    }
}
