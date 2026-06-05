package com.llosa.backend.proyecto.controller;

import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.service.PisoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PisoController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class PisoControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean PisoService pisoService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    @Test
    void getPisosByTorre_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/pisos/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPisosByTorre_autenticado_devuelveLista() throws Exception {
        Piso piso = Piso.builder().id(1L).nroPiso(3).build();
        when(pisoService.findByTorre(1L, null)).thenReturn(List.of(piso));

        mockMvc.perform(get("/api/pisos/1")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nroPiso").value(3));
    }

    @Test
    void getPisosByTorre_conSearch_filtraResultados() throws Exception {
        Piso piso = Piso.builder().id(1L).nroPiso(3).build();
        when(pisoService.findByTorre(1L, "3")).thenReturn(List.of(piso));

        mockMvc.perform(get("/api/pisos/1?search=3")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPisosByTorre_vacia_devuelveArrayVacio() throws Exception {
        when(pisoService.findByTorre(99L, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/pisos/99")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
