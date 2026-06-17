package com.llosa.backend.proyecto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.dto.request.CrearContratoDTO;
import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.dto.response.UsuarioActivoResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.service.UsuarioActivoService;
import com.llosa.backend.seguridad.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
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

@WebMvcTest(UsuarioActivoController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class UsuarioActivoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UsuarioActivoService usuarioActivoService;
    @MockitoBean UsuarioService usuarioService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    private Piso buildPiso() {
        var proyecto = Proyecto.builder().id(UUID.randomUUID()).nombre("Test Proyecto").build();
        var torre = Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();
        return Piso.builder().id(1L).nroPiso(1).torre(torre).build();
    }

    private Activo buildActivo() {
        return Activo.builder()
                .id(UUID.randomUUID()).nro("DPTO 101")
                .tipo(TipoActivo.DEPARTAMENTO).areaM2(BigDecimal.valueOf(80))
                .estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.valueOf(200000)).piso(buildPiso())
                .build();
    }

    @Test
    void obtenerMisActivos_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/expedientes/mis-activos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerMisActivos_autenticado_devuelveLista() throws Exception {
        var activo = buildActivo();
        var usuarioLogueado = new com.llosa.backend.seguridad.entity.Usuario();
        usuarioLogueado.setId(1);
        usuarioLogueado.setFirebaseUuid("test-uid");

        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .activos(List.of(activo))
                .build();

        when(usuarioService.findByFirebaseUuid("test-uid")).thenReturn(usuarioLogueado);
        when(usuarioActivoService.findByUsuario(1)).thenReturn(List.of(ua));

        mockMvc.perform(get("/api/expedientes/mis-activos")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nro").value("DPTO 101"));
    }

    @Test
    void obtenerTodos_devuelvePaginado() throws Exception {
        var activo = buildActivo();
        var response = UsuarioActivoResponseDTO.fromEntity(
                UsuarioActivo.builder()
                        .uuidUsuarioActivo(UUID.randomUUID())
                        .activos(List.of(activo))
                        .clientes(List.of())
                        .build()
        );
        Page<UsuarioActivoResponseDTO> page = new PageImpl<>(List.of(response));
        when(usuarioActivoService.listar(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/expedientes")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }

    @Test
    void crearContratoBase_devuelve201() throws Exception {
        var dto = new CrearContratoDTO(List.of(1), "Credito Directo", "SEPARACION", null, null);
        var activo = buildActivo();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .activos(List.of(activo))
                .clientes(List.of())
                .tipoFinanciamiento("Credito Directo")
                .build();
        when(usuarioActivoService.crearContratoBase(any())).thenReturn(ua);

        mockMvc.perform(post("/api/expedientes/crear")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoFinanciamiento").value("Credito Directo"));
    }

    @Test
    void asignarActivosAContrato_devuelve200() throws Exception {
        var dto = new AsignarActivoDTO(UUID.randomUUID(), List.of(UUID.randomUUID()));
        var activo = buildActivo();
        var response = UsuarioActivoResponseDTO.fromEntity(
                UsuarioActivo.builder()
                        .uuidUsuarioActivo(UUID.randomUUID())
                        .activos(List.of(activo))
                        .clientes(List.of())
                        .build()
        );
        when(usuarioActivoService.asignarActivo(any())).thenReturn(response);

        mockMvc.perform(post("/api/expedientes/asignar")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void verContratoActivoUsuario_existe_devuelve200() throws Exception {
        UUID activoId = UUID.randomUUID();
        var activo = buildActivo();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .activos(List.of(activo))
                .clientes(List.of())
                .build();
        when(usuarioActivoService.findByActivo(activoId)).thenReturn(Optional.of(ua));

        mockMvc.perform(get("/api/expedientes/{uuidActivo}/contrato", activoId)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }

    @Test
    void verContratoActivoUsuario_noExiste_devuelve404() throws Exception {
        UUID activoId = UUID.randomUUID();
        when(usuarioActivoService.findByActivo(activoId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/expedientes/{uuidActivo}/contrato", activoId)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerActivosPorUsuario_devuelveLista() throws Exception {
        var activo = buildActivo();
        var ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .activos(List.of(activo))
                .clientes(List.of())
                .build();
        when(usuarioActivoService.findByUsuario(1)).thenReturn(List.of(ua));

        mockMvc.perform(get("/api/expedientes/{id_usuario}", 1)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerSoloActivosPorUsuario_devuelveLista() throws Exception {
        var activo = buildActivo();
        when(usuarioActivoService.findByUsuarioId(1)).thenReturn(List.of(activo));

        mockMvc.perform(get("/api/expedientes/usuario/{id_usuario}/activos", 1)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nro").value("DPTO 101"));
    }

    @Test
    void eliminarContrato_devuelve204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(usuarioActivoService).eliminarContrato(id);

        mockMvc.perform(delete("/api/expedientes/delete/{uuidUsuarioActivo}", id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
