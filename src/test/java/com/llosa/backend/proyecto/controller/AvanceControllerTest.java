package com.llosa.backend.proyecto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.proyecto.dto.request.HitoPisoUpdateDTO;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.service.HitoPisoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AvanceController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class AvanceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean HitoPisoService hitoPisoService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    @Test
    void actualizarAvance_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(put("/api/avances-unidad/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarAvance_completado_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        Hito hito = Hito.builder().id(UUID.randomUUID()).titulo("H1").orden(1)
                .estado(EstadoHito.COMPLETADO).build();
        HitoPiso hitoPiso = HitoPiso.builder().id(id).hito(hito)
                .piso(Piso.builder().id(1L).nroPiso(1).build())
                .estado(EstadoHito.COMPLETADO).build();

        when(hitoPisoService.cambiarEstado(id, EstadoHito.COMPLETADO)).thenReturn(hitoPiso);

        HitoPisoUpdateDTO dto = new HitoPisoUpdateDTO(EstadoHito.COMPLETADO);

        mockMvc.perform(put("/api/avances-unidad/" + id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarAvance_sinEstado_devuelve400() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/avances-unidad/" + id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

}
