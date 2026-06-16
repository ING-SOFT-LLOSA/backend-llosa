package com.llosa.backend.agenda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.agenda.dto.request.RespuestaClienteRequest;
import com.llosa.backend.agenda.dto.response.CitaResponse;
import com.llosa.backend.agenda.service.AgendaService;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgendaClienteController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class AgendaClienteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AgendaService agendaService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    private static FirebaseAuthenticationToken clienteToken() {
        return new FirebaseAuthenticationToken("cliente-uid", "cliente@test.com",
                List.of(new SimpleGrantedAuthority("AGENDA_VER")));
    }

    @Test
    void listarMisCitas_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/agenda/cliente/citas"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarMisCitas_autenticado_devuelveLista() throws Exception {
        when(agendaService.listarCitasCliente("cliente-uid")).thenReturn(List.of());

        mockMvc.perform(get("/api/agenda/cliente/citas")
                        .with(authentication(clienteToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listarProximasCitas_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/agenda/cliente/citas/proximas"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarProximasCitas_autenticado_devuelveLista() throws Exception {
        when(agendaService.listarProximasCitasCliente("cliente-uid")).thenReturn(List.of());

        mockMvc.perform(get("/api/agenda/cliente/citas/proximas")
                        .with(authentication(clienteToken())))
                .andExpect(status().isOk());
    }

    @Test
    void responderCita_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(patch("/api/agenda/cliente/citas/{id}/respuesta", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void responderCita_autenticado_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        var req = new RespuestaClienteRequest(true, "Confirmo asistencia");
        when(agendaService.responderCita(eq(id), eq("cliente-uid"), any())).thenReturn(mock(CitaResponse.class));

        mockMvc.perform(patch("/api/agenda/cliente/citas/{id}/respuesta", id)
                        .with(authentication(clienteToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void proponerDisponibilidad_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(post("/api/agenda/cliente/citas/{id}/disponibilidad", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void proponerDisponibilidad_autenticado_devuelveLista() throws Exception {
        UUID id = UUID.randomUUID();
        var json = """
                {"bloques":[{"inicio":"2026-07-03T10:00:00","fin":"2026-07-03T11:00:00"}]}
                """;
        when(agendaService.proponerDisponibilidad(eq(id), eq("cliente-uid"), any())).thenReturn(List.of());

        mockMvc.perform(post("/api/agenda/cliente/citas/{id}/disponibilidad", id)
                        .with(authentication(clienteToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}
