package com.llosa.backend.proyecto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.proyecto.dto.request.HitoCreateDTO;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.enums.TipoHito;
import com.llosa.backend.proyecto.service.HitoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HitoController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class HitoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean HitoService hitoService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    private Hito buildHito() {
        Proyecto proyecto = Proyecto.builder().id(UUID.randomUUID()).nombre("Test").build();
        return Hito.builder()
                .id(UUID.randomUUID()).titulo("Cimentación").orden(1)
                .tipo(TipoHito.OBRA).estado(EstadoHito.PENDIENTE).proyecto(proyecto)
                .build();
    }

    @Test
    void actualizarHito_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(put("/api/hitos/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarHito_valido_devuelve200() throws Exception {
        Hito hito = buildHito();
        when(hitoService.findById(hito.getId())).thenReturn(hito);
        when(hitoService.save(hito)).thenReturn(hito);

        HitoCreateDTO dto = new HitoCreateDTO("Cimentación Updated", 1, TipoHito.OBRA, null);

        mockMvc.perform(put("/api/hitos/" + hito.getId())
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Cimentación Updated"));
    }

    @Test
    void deleteHito_valido_devuelve204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(hitoService).deleteById(id);

        mockMvc.perform(delete("/api/hitos/" + id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
