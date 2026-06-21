package com.llosa.backend.comercial.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.comercial.dto.EtapaExpedienteEstadoRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteResponse;
import com.llosa.backend.comercial.enums.EstadoEtapaExpediente;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.service.EtapaExpedienteService;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EtapaExpedienteController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class EtapaExpedienteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean EtapaExpedienteService etapaExpedienteService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    private EtapaExpedienteResponse buildResponse(UUID id, UUID usuarioActivoId) {
        return new EtapaExpedienteResponse(
                id, usuarioActivoId, EtapaProceso.CONTRATO,
                EstadoEtapaExpediente.PENDIENTE, 2, 3);
    }

    @Test
    void listarPorExpediente_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/etapa-expediente/expediente/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarPorExpediente_autenticado_devuelveLista() throws Exception {
        UUID usuarioActivoId = UUID.randomUUID();
        EtapaExpedienteResponse response = buildResponse(UUID.randomUUID(), usuarioActivoId);
        when(etapaExpedienteService.listarPorUsuarioActivo(usuarioActivoId)).thenReturn(List.of(response));

        mockMvc.perform(get("/etapa-expediente/expediente/" + usuarioActivoId)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].etapaProceso").value("CONTRATO"));
    }

    @Test
    void obtenerPorId_autenticado_devuelveEtapa() throws Exception {
        UUID id = UUID.randomUUID();
        EtapaExpedienteResponse response = buildResponse(id, UUID.randomUUID());
        when(etapaExpedienteService.obtenerPorId(id)).thenReturn(response);

        mockMvc.perform(get("/etapa-expediente/" + id)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuidEtapaExpediente").value(id.toString()));
    }

    @Test
    void crear_valido_devuelve201() throws Exception {
        UUID usuarioActivoId = UUID.randomUUID();
        EtapaExpedienteResponse response = buildResponse(UUID.randomUUID(), usuarioActivoId);
        EtapaExpedienteRequest request = new EtapaExpedienteRequest(EtapaProceso.CONTRATO, EstadoEtapaExpediente.PENDIENTE);
        when(etapaExpedienteService.crear(eq(usuarioActivoId), any(EtapaExpedienteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/etapa-expediente/expediente/" + usuarioActivoId)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void actualizar_valido_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        EtapaExpedienteResponse response = buildResponse(id, UUID.randomUUID());
        EtapaExpedienteRequest request = new EtapaExpedienteRequest(EtapaProceso.CONTRATO, EstadoEtapaExpediente.COMPLETADO);
        when(etapaExpedienteService.actualizar(eq(id), any(EtapaExpedienteRequest.class))).thenReturn(response);

        mockMvc.perform(put("/etapa-expediente/" + id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarEstado_valido_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        EtapaExpedienteResponse response = buildResponse(id, UUID.randomUUID());
        EtapaExpedienteEstadoRequest request = new EtapaExpedienteEstadoRequest(EstadoEtapaExpediente.COMPLETADO);
        when(etapaExpedienteService.actualizarEstado(eq(id), any(EtapaExpedienteEstadoRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/etapa-expediente/" + id + "/estado")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void eliminar_autenticado_devuelve204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(etapaExpedienteService).eliminar(id);

        mockMvc.perform(delete("/etapa-expediente/" + id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
