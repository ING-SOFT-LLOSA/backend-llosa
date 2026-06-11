package com.llosa.backend.proyecto.controller;

import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.service.TorreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TorreController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class TorreControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean TorreService torreService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    @Test
    void getTorresByProyecto_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/torres/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTorresByProyecto_autenticado_devuelveLista() throws Exception {
        UUID proyectoId = UUID.randomUUID();
        Proyecto proyecto = Proyecto.builder().id(proyectoId).nombre("Test").build();
        Torre torre = Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();
        when(torreService.findByProyecto(proyectoId, null)).thenReturn(List.of(torre));

        mockMvc.perform(get("/api/torres/" + proyectoId)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Torre A"));
    }

    @Test
    void getTorresByProyecto_conSearch_filtraResultados() throws Exception {
        UUID proyectoId = UUID.randomUUID();
        Proyecto proyecto = Proyecto.builder().id(proyectoId).nombre("Test").build();
        Torre torre = Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();
        when(torreService.findByProyecto(proyectoId, "A")).thenReturn(List.of(torre));

        mockMvc.perform(get("/api/torres/" + proyectoId + "?search=A")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
