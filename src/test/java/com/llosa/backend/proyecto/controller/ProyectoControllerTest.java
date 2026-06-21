package com.llosa.backend.proyecto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.proyecto.dto.request.HitoCreateDTO;
import com.llosa.backend.proyecto.dto.request.ProyectoCargaDTO;
import com.llosa.backend.proyecto.dto.request.ProyectoCreateDTO;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.service.HitoService;
import com.llosa.backend.proyecto.service.ProyectoService;
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

@WebMvcTest(ProyectoController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class ProyectoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ProyectoService proyectoService;
    @MockitoBean HitoService hitoService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    private Proyecto buildProyecto() {
        return Proyecto.builder()
                .id(UUID.randomUUID())
                .nombre("Torre Sol")
                .descripcion("Proyecto test")
                .precertificacionEdgeLeed(false)
                .departamento("Lima")
                .distrito("Miraflores")
                .direccion("Av. Test 123")
                .build();
    }

    @Test
    void findAll_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/proyectos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAll_autenticado_devuelveListaProyectos() throws Exception {
        Proyecto proyecto = buildProyecto();
        when(proyectoService.findAll(null)).thenReturn(List.of(proyecto));

        mockMvc.perform(get("/api/proyectos")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Torre Sol"));
    }

    @Test
    void findAll_conSearch_lllamaBusquedaFiltrada() throws Exception {
        Proyecto proyecto = buildProyecto();
        when(proyectoService.findAll("sol")).thenReturn(List.of(proyecto));

        mockMvc.perform(get("/api/proyectos?search=sol")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void crearProyecto_valido_devuelve201() throws Exception {
        Proyecto proyecto = buildProyecto();
        when(proyectoService.save(any(Proyecto.class))).thenReturn(proyecto);

        ProyectoCreateDTO dto = new ProyectoCreateDTO(
                "Torre Sol", "Desc", false, null, "Lima", "Miraflores",
                "Av. Test 123", null, null);

        mockMvc.perform(post("/api/proyectos")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Torre Sol"));
    }

    @Test
    void crearProyecto_sinNombre_devuelve400() throws Exception {
        ProyectoCreateDTO dto = new ProyectoCreateDTO(
                "", "Desc", false, null, "Lima", "Miraflores",
                "Av. Test 123", null, null);

        mockMvc.perform(post("/api/proyectos")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarProyecto_valido_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        Proyecto proyecto = buildProyecto();
        proyecto.setId(id);
        when(proyectoService.findById(id)).thenReturn(proyecto);
        when(proyectoService.save(proyecto)).thenReturn(proyecto);

        ProyectoCreateDTO dto = new ProyectoCreateDTO(
                "Torre Sol Updated", "Desc", false, null, "Lima", "Miraflores",
                "Av. Test 456", null, null);

        mockMvc.perform(put("/api/proyectos/" + id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void eliminarProyecto_devuelve204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(proyectoService).deleteById(id);

        mockMvc.perform(delete("/api/proyectos/" + id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAvanceGeneral_devuelveDTO() throws Exception {
        UUID id = UUID.randomUUID();
        Proyecto proyecto = buildProyecto();
        proyecto.setId(id);
        when(proyectoService.findById(id)).thenReturn(proyecto);
        when(proyectoService.getPorcentajeAvance(id)).thenReturn(75.0);

        mockMvc.perform(get("/api/proyectos/" + id + "/avance-general")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeAvance").value(75.0));
    }

    @Test
    void crearHito_valido_devuelve201() throws Exception {
        UUID proyectoId = UUID.randomUUID();
        Proyecto proyecto = buildProyecto();
        proyecto.setId(proyectoId);

        Hito hito = Hito.builder()
                .id(UUID.randomUUID())
                .titulo("Cimentación")
                .orden(1)
                .estado(EstadoHito.PENDIENTE)
                .proyecto(proyecto)
                .build();
        when(hitoService.save(eq(proyectoId), any(Hito.class))).thenReturn(hito);

        HitoCreateDTO dto = new HitoCreateDTO("Cimentación", 1, null);

        mockMvc.perform(post("/api/proyectos/" + proyectoId + "/hitos")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Cimentación"));
    }

    @Test
    void crearEstructuraFisica_devuelve200() throws Exception {
        UUID proyectoId = UUID.randomUUID();
        doNothing().when(proyectoService).cargarProyecto(eq(proyectoId), any());

        ProyectoCargaDTO cargaDTO = new ProyectoCargaDTO(List.of());

        mockMvc.perform(post("/api/proyectos/" + proyectoId + "/estructura-fisica")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cargaDTO)))
                .andExpect(status().isOk());
    }
}
