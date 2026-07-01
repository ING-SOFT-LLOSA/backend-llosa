package com.llosa.backend.proyecto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.proyecto.dto.request.ReporteCreateRequest;
import com.llosa.backend.proyecto.dto.request.ReporteUpdateRequest;
import com.llosa.backend.proyecto.dto.response.ReporteResponse;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.ReporteService;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReporteController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class ReporteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ReporteService reporteService;
    @MockitoBean ActivoService activoService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    @MockitoBean UsuarioService usuarioService;

    private Piso buildPiso() {
        var proyecto = Proyecto.builder().id(UUID.randomUUID()).nombre("Test Proyecto").build();
        var torre = Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();
        return Piso.builder().id(1L).nroPiso(1).torre(torre).build();
    }

    @Test
    void crear_sinAutenticar_devuelve403() throws Exception {
        var req = new ReporteCreateRequest(UUID.randomUUID(), "Enero 2026", null, null, null);

        mockMvc.perform(post("/api/reportes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
    @Test
    void crear_autenticado_devuelve201() throws Exception {
        UUID proyectoId = UUID.randomUUID();
        var req = new ReporteCreateRequest(proyectoId, "Enero 2026", "Desc", null, List.of("Hito1"));
        var response = new ReporteResponse(UUID.randomUUID(), proyectoId, "Test", "Enero 2026",
                BigDecimal.ZERO, "Desc", List.of("Hito1"), LocalDateTime.now(), List.of());

        Usuario usuarioFalso = new Usuario();
        usuarioFalso.setId(1);
        when(usuarioService.findByFirebaseUuid("firebase-test-uid")).thenReturn(usuarioFalso);
        when(reporteService.crear(any(ReporteCreateRequest.class), any(), any()))
                .thenReturn(response);

        MockMultipartFile reportePart = new MockMultipartFile(
                "reporte",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(req)
        );

        // 2. CAMBIO CLAVE: Pasamos un String como Principal, no un Integer
        var authCustom = new TestingAuthenticationToken("firebase-test-uid", null, TestData.proyectoAuthToken().getAuthorities());
        authCustom.setAuthenticated(true);

        mockMvc.perform(multipart("/api/reportes")
                        .file(reportePart)
                        .with(authentication(authCustom))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tituloPeriodo").value("Enero 2026"));
    }
    
    @Test
    void crear_sinTitulo_devuelve400() throws Exception {
        // 1. Creamos el request con el título inválido/vacío
        var req = new ReporteCreateRequest(UUID.randomUUID(), "", null, null, null);

        // 2. Lo envolvemos en la parte multipart "reporte" como JSON
        MockMultipartFile reportePart = new MockMultipartFile(
                "reporte",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(req)
        );

        // 3. Forzamos un token con Integer para evitar el ClassCastException que vimos antes
        var authCustom = new org.springframework.security.authentication.TestingAuthenticationToken(
                1, null, TestData.proyectoAuthToken().getAuthorities());
        authCustom.setAuthenticated(true);

        // 4. Ejecutamos la petición como multipart
        mockMvc.perform(multipart("/api/reportes")
                        .file(reportePart)
                        .with(authentication(authCustom))
                        .with(csrf()))
                .andExpect(status().isBadRequest()); // Ahora sí saltará el 400 de validación
    }

    @Test
    void obtenerPorId_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        var response = new ReporteResponse(id, UUID.randomUUID(), "Test", "Enero 2026",
                BigDecimal.ZERO, null, List.of(), LocalDateTime.now(), List.of());
        when(reporteService.obtenerPorId(id)).thenReturn(response);

        mockMvc.perform(get("/api/reportes/{id}", id)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void listarPorProyecto_devuelvePaginado() throws Exception {
        UUID proyectoId = UUID.randomUUID();
        var response = new ReporteResponse(UUID.randomUUID(), proyectoId, "Test", "Enero 2026",
                BigDecimal.ZERO, null, List.of(), LocalDateTime.now(), List.of());
        Page<ReporteResponse> page = new PageImpl<>(List.of(response));
        when(reporteService.listarPorProyecto(eq(proyectoId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/reportes/proyecto/{uuidProyecto}", proyectoId)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }

    @Test
    void listarPorActivoProyecto_devuelvePaginado() throws Exception {
        UUID activoId = UUID.randomUUID();
        var piso = buildPiso();
        var activo = Activo.builder().id(activoId).piso(piso).build();
        when(activoService.findById(activoId)).thenReturn(activo);

        UUID proyectoId = piso.getTorre().getProyecto().getId();
        var response = new ReporteResponse(UUID.randomUUID(), proyectoId, "Test", "Enero 2026",
                BigDecimal.ZERO, null, List.of(), LocalDateTime.now(), List.of());
        Page<ReporteResponse> page = new PageImpl<>(List.of(response));
        when(reporteService.listarPorProyecto(eq(proyectoId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/reportes/proyecto/{uuidActivo}/activo", activoId)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }

    @Test
    void actualizar_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        var req = new ReporteUpdateRequest("Febrero 2026", "Actualizado", null, List.of("Hito1"));
        var response = new ReporteResponse(id, UUID.randomUUID(), "Test", "Febrero 2026",
                new BigDecimal("50.00"), "Actualizado", List.of("Hito1"), LocalDateTime.now(), List.of());
        when(reporteService.actualizar(eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/api/reportes/{id}", id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tituloPeriodo").value("Febrero 2026"));
    }

    @Test
    void eliminar_devuelve204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(reporteService).eliminar(id);

        mockMvc.perform(delete("/api/reportes/{id}", id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
