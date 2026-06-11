package com.llosa.backend.comercial.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.comercial.dto.HitoComercialRequest;
import com.llosa.backend.comercial.dto.HitoComercialResponse;
import com.llosa.backend.comercial.dto.StepperResponse;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.service.HitoComercialService;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HitoComercialController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class HitoComercialControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean HitoComercialService hitoComercialService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    @Test
    void crearHito_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(post("/api/comercial/hitos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearHito_valido_devuelve201() throws Exception {
        UUID uaId = UUID.randomUUID();
        UUID hitoId = UUID.randomUUID();
        HitoComercialResponse response = HitoComercialResponse.builder()
                .uuidHitoComercial(hitoId).uuidUsuarioActivo(uaId)
                .etapaProceso(EtapaProceso.SEPARACION).nombreHito("Hito 1")
                .descripcion("Desc").orden(1).estado(EstadoHitoComercial.PENDIENTE)
                .build();
        when(hitoComercialService.crearHito(any())).thenReturn(response);

        HitoComercialRequest req = new HitoComercialRequest(
                uaId, EtapaProceso.SEPARACION, "Hito 1", "Desc", 1);

        mockMvc.perform(post("/api/comercial/hitos")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreHito").value("Hito 1"));
    }

    @Test
    void crearHito_sinNombreHito_devuelve400() throws Exception {
        HitoComercialRequest req = new HitoComercialRequest(
                UUID.randomUUID(), EtapaProceso.SEPARACION, "", "Desc", 1);

        mockMvc.perform(post("/api/comercial/hitos")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarHito_encontrado_devuelve204() throws Exception {
        UUID hitoId = UUID.randomUUID();
        doNothing().when(hitoComercialService).eliminarHito(hitoId);

        mockMvc.perform(delete("/api/comercial/hitos/" + hitoId)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarHito_noEncontrado_devuelve404() throws Exception {
        UUID hitoId = UUID.randomUUID();
        doThrow(new RecursoNoEncontradoException("Hito no encontrado"))
                .when(hitoComercialService).eliminarHito(hitoId);

        mockMvc.perform(delete("/api/comercial/hitos/" + hitoId)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarEstado_completado_devuelve200() throws Exception {
        UUID hitoId = UUID.randomUUID();
        UUID uaId = UUID.randomUUID();
        HitoComercialResponse response = HitoComercialResponse.builder()
                .uuidHitoComercial(hitoId).uuidUsuarioActivo(uaId)
                .etapaProceso(EtapaProceso.SEPARACION).nombreHito("Hito 1")
                .orden(1).estado(EstadoHitoComercial.COMPLETADO)
                .build();
        when(hitoComercialService.actualizarEstado(hitoId, EstadoHitoComercial.COMPLETADO))
                .thenReturn(response);

        mockMvc.perform(patch("/api/comercial/hitos/" + hitoId + "/estado?estado=COMPLETADO")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
    }

    @Test
    void obtenerStepper_devuelve200() throws Exception {
        UUID uaId = UUID.randomUUID();
        StepperResponse response = StepperResponse.builder()
                .uuidUsuarioActivo(uaId).etapas(List.of()).build();
        when(hitoComercialService.obtenerStepper(uaId)).thenReturn(response);

        mockMvc.perform(get("/api/comercial/stepper/" + uaId)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }
}
