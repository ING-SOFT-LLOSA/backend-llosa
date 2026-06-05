package com.llosa.backend.seguridad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.seguridad.dto.UpdateUsuarioDTO;
import com.llosa.backend.seguridad.dto.UsuarioResponse;
import com.llosa.backend.seguridad.dto.UsuarioResponseFunciones;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class})
class UsuarioControllerExtendedTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UsuarioService usuarioService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;
    @MockitoBean com.llosa.backend.seguridad.repository.RolRepository rolRepository;

    @Test
    void listarPaginado_devuelvePaginaDeUsuarios() throws Exception {
        UsuarioResponseFunciones dto = UsuarioResponseFunciones.fromEntity(TestData.usuario());
        Page<UsuarioResponseFunciones> page = new PageImpl<>(List.of(dto));
        when(usuarioService.listarPaginadoYFiltrado(eq(""), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/users/paginado")
                        .with(authentication(TestData.authToken())))
                .andExpect(status().isOk());
    }

    @Test
    void listarPaginado_conSearch_filtraResultados() throws Exception {
        Page<UsuarioResponseFunciones> page = new PageImpl<>(List.of());
        when(usuarioService.listarPaginadoYFiltrado(eq("juan"), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/users/paginado?search=juan")
                        .with(authentication(TestData.authToken())))
                .andExpect(status().isOk());
    }

    @Test
    void buscarUsuarioPorId_encontrado_devuelve200() throws Exception {
        Usuario u = TestData.usuario();
        u.setId(1);
        UsuarioResponse response = new UsuarioResponse();
        response.setId(1);
        response.setNombre("Juan");

        when(usuarioService.findById(1)).thenReturn(u);
        when(usuarioService.toResponse(u)).thenReturn(response);

        mockMvc.perform(get("/api/users/1")
                        .with(authentication(TestData.authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan"));
    }

    @Test
    void modificarInformacionUsuario_valido_devuelve200() throws Exception {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(1);
        response.setNombre("Juan Updated");

        UpdateUsuarioDTO updateDTO = new UpdateUsuarioDTO();
        updateDTO.setNombre("Juan Updated");
        updateDTO.setApellidos("Perez");
        updateDTO.setEmail("juan@test.com");

        when(usuarioService.actualizarUsuario(eq(1), any(UpdateUsuarioDTO.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/users/1")
                        .with(authentication(TestData.authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan Updated"));
    }

    @Test
    void eliminarCompletamente_devuelve200() throws Exception {
        doNothing().when(usuarioService).eliminarCompletamente(1);

        mockMvc.perform(delete("/api/users/1/hard")
                        .with(authentication(TestData.authToken()))
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
