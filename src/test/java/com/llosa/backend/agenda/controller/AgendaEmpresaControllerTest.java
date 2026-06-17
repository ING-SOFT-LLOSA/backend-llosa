package com.llosa.backend.agenda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.agenda.dto.request.ActualizarCitaRequest;
import com.llosa.backend.agenda.dto.request.SeleccionarBloqueRequest;
import com.llosa.backend.agenda.dto.response.CitaResponse;
import com.llosa.backend.agenda.enums.EstadoCita;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgendaEmpresaController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class AgendaEmpresaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AgendaService agendaService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    private static FirebaseAuthenticationToken agendaToken() {
        return new FirebaseAuthenticationToken("gestor-uid", "gestor@test.com",
                List.of(
                        new SimpleGrantedAuthority("AGENDA_VER"),
                        new SimpleGrantedAuthority("AGENDA_CREAR"),
                        new SimpleGrantedAuthority("AGENDA_EDITAR")
                ));
    }

    private static FirebaseAuthenticationToken adminToken() {
        return new FirebaseAuthenticationToken("admin-uid", "admin@test.com",
                List.of(new SimpleGrantedAuthority("ADMIN_TOTAL")));
    }

    @Test
    void crearCita_sinAutenticar_devuelve403() throws Exception {
        var json = """
                {"clienteId":1,"activoId":"%s","tipoEvento":"ENTREGA_LLAVES","titulo":"Titulo","fechaInicio":"2026-07-01T10:00:00","fechaFin":"2026-07-01T11:00:00","permiteReprogramacion":true,"clienteUsaGoogle":false}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/agenda/empresa/citas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearCita_autenticado_devuelve201() throws Exception {
        var json = """
                {"clienteId":1,"activoId":"%s","tipoEvento":"ENTREGA_LLAVES","titulo":"Titulo","fechaInicio":"2026-07-01T10:00:00","fechaFin":"2026-07-01T11:00:00","permiteReprogramacion":true,"clienteUsaGoogle":false}
                """.formatted(UUID.randomUUID());
        when(agendaService.crearCita(anyString(), any())).thenReturn(mock(CitaResponse.class));

        mockMvc.perform(post("/api/agenda/empresa/citas")
                        .with(authentication(agendaToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void actualizarCita_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        var req = new ActualizarCitaRequest("Nuevo titulo", null, null, null, null, EstadoCita.CONFIRMADA, null, null);
        when(agendaService.actualizarCita(eq(id), any())).thenReturn(mock(CitaResponse.class));

        mockMvc.perform(put("/api/agenda/empresa/citas/{id}", id)
                        .with(authentication(agendaToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void cancelarCita_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        when(agendaService.cancelarCita(id, "Cancelado por la empresa")).thenReturn(mock(CitaResponse.class));

        mockMvc.perform(delete("/api/agenda/empresa/citas/{id}", id)
                        .with(authentication(agendaToken()))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void cancelarCita_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(delete("/api/agenda/empresa/citas/{id}", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void seleccionarBloque_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        var req = new SeleccionarBloqueRequest(1L);
        when(agendaService.seleccionarBloqueDisponibilidad(eq(id), any())).thenReturn(mock(CitaResponse.class));

        mockMvc.perform(patch("/api/agenda/empresa/citas/{id}/seleccionar-bloque", id)
                        .with(authentication(agendaToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void listarPorActivo_devuelveLista() throws Exception {
        UUID activoId = UUID.randomUUID();
        when(agendaService.listarCitasPorActivo(activoId)).thenReturn(List.of());

        mockMvc.perform(get("/api/agenda/empresa/citas/activo/{activoId}", activoId)
                        .with(authentication(agendaToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listarCalendarioGestor_devuelveLista() throws Exception {
        var inicio = LocalDateTime.of(2026, 7, 1, 0, 0);
        var fin = LocalDateTime.of(2026, 7, 31, 23, 59);
        when(agendaService.listarCitasGestor("gestor-uid", inicio, fin)).thenReturn(List.of());

        mockMvc.perform(get("/api/agenda/empresa/citas/calendario")
                        .param("inicio", inicio.toString())
                        .param("fin", fin.toString())
                        .with(authentication(agendaToken())))
                .andExpect(status().isOk());
    }

    @Test
    void listarTodas_sinAdmin_devuelve403() throws Exception {
        mockMvc.perform(get("/api/agenda/empresa/citas")
                        .with(authentication(agendaToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarTodas_conAdmin_devuelveLista() throws Exception {
        when(agendaService.listarTodasLasCitas()).thenReturn(List.of());

        mockMvc.perform(get("/api/agenda/empresa/citas")
                        .with(authentication(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void sincronizarManual_sinAdmin_devuelve403() throws Exception {
        mockMvc.perform(post("/api/agenda/empresa/sincronizar")
                        .with(authentication(agendaToken()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void sincronizarManual_conAdmin_devuelveOk() throws Exception {
        doNothing().when(agendaService).reintentarSincronizacionesPendientes();

        mockMvc.perform(post("/api/agenda/empresa/sincronizar")
                        .with(authentication(adminToken()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").isString());
    }
}
