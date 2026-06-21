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

@WebMvcTest(HitoComercialController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class HitoComercialControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean HitoComercialService hitoComercialService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    private HitoComercialResponse buildResponse(UUID id) {
        return HitoComercialResponse.builder()
                .uuidHitoComercial(id)
                .uuidEtapaExpediente(UUID.randomUUID())
                .etapaProceso(EtapaProceso.CONTRATO)
                .nombreHito("Firma de contrato")
                .descripcion("Hito de firma")
                .orden(1)
                .estado(EstadoHitoComercial.PENDIENTE)
                .build();
    }

    @Test
    void crearHito_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(post("/api/comercial/hitos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearHito_valido_devuelve201() throws Exception {
        HitoComercialResponse response = buildResponse(UUID.randomUUID());
        HitoComercialRequest request = HitoComercialRequest.builder()
                .uuidEstapaExpediente(UUID.randomUUID())
                .nombreHito("Firma de contrato")
                .descripcion("Hito de firma")
                .orden(1)
                .build();
        when(hitoComercialService.crearHito(any(HitoComercialRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/comercial/hitos")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreHito").value("Firma de contrato"));
    }

    @Test
    void eliminarHito_autenticado_devuelve204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(hitoComercialService).eliminarHito(id);

        mockMvc.perform(delete("/api/comercial/hitos/" + id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void actualizarEstado_valido_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        HitoComercialResponse response = buildResponse(id);
        when(hitoComercialService.actualizarEstado(eq(id), eq(EstadoHitoComercial.COMPLETADO))).thenReturn(response);

        mockMvc.perform(patch("/api/comercial/hitos/" + id + "/estado")
                        .param("estado", "COMPLETADO")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerStepper_autenticado_devuelveStepper() throws Exception {
        UUID usuarioActivoId = UUID.randomUUID();
        StepperResponse response = StepperResponse.builder()
                .uuidUsuarioActivo(usuarioActivoId)
                .etapas(List.of())
                .build();
        when(hitoComercialService.obtenerStepper(usuarioActivoId)).thenReturn(response);

        mockMvc.perform(get("/api/comercial/stepper/" + usuarioActivoId)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuidUsuarioActivo").value(usuarioActivoId.toString()));
    }
}
