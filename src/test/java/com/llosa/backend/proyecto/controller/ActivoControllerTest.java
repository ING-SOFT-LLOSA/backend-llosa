package com.llosa.backend.proyecto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.proyecto.dto.request.ActivoRequestDTO;
import com.llosa.backend.proyecto.dto.response.AvanceUnidadResponsePorcentajeDTO;
import com.llosa.backend.proyecto.dto.response.SeguimientoResponseDTO;
import com.llosa.backend.proyecto.dto.shared.FaseActualDTO;
import com.llosa.backend.proyecto.dto.shared.PasoStepperDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.HitoPisoService;
import com.llosa.backend.proyecto.service.SeguimientoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivoController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class ActivoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean HitoPisoService hitoPisoService;
    @MockitoBean ActivoService activoService;
    @MockitoBean SeguimientoService seguimientoService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    private Piso buildPiso() {
        Proyecto proyecto = Proyecto.builder().nombre("Proyecto Test").build();
        Torre torre = Torre.builder().id(1L).nombre("Torre A").proyecto(proyecto).build();
        return Piso.builder().id(1L).nroPiso(1).torre(torre).build();
    }

    private Activo buildActivo() {
        return Activo.builder()
                .id(UUID.randomUUID()).nro("DPTO 101").tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(BigDecimal.valueOf(80)).estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .precio(BigDecimal.valueOf(200000)).descripcion("Test").piso(buildPiso())
                .build();
    }

    @Test
    void getActivosByPiso_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/activos/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActivosByPiso_autenticado_devuelveLista() throws Exception {
        Activo activo = buildActivo();
        when(activoService.findByPiso(1L, null)).thenReturn(List.of(activo));

        mockMvc.perform(get("/api/activos/1")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nro").value("DPTO 101"));
    }

    @Test
    void crearActivo_valido_devuelve200() throws Exception {
        Activo activo = buildActivo();
        when(activoService.saveIndividual(eq(1L), any(Activo.class))).thenReturn(activo);

        ActivoRequestDTO dto = new ActivoRequestDTO("DPTO 101", TipoActivo.DEPARTAMENTO,
                BigDecimal.valueOf(80), EstadoComercialActivo.DISPONIBLE,
                BigDecimal.valueOf(200000), "Test");

        mockMvc.perform(post("/api/activos/1/pisos")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nro").value("DPTO 101"));
    }

    @Test
    void actualizarActivo_valido_devuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        Activo activo = buildActivo();
        activo.setId(id);
        when(activoService.findById(id)).thenReturn(activo);
        when(activoService.save(activo)).thenReturn(activo);

        ActivoRequestDTO dto = new ActivoRequestDTO("DPTO 102", TipoActivo.DEPARTAMENTO,
                BigDecimal.valueOf(90), EstadoComercialActivo.DISPONIBLE,
                BigDecimal.valueOf(220000), "Actualizado");

        mockMvc.perform(put("/api/activos/" + id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void eliminarActivo_devuelve204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(activoService).deleteById(id);

        mockMvc.perform(delete("/api/activos/" + id)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getHitos_devuelveLista() throws Exception {
        UUID activoId = UUID.randomUUID();
        Hito hito = Hito.builder().id(UUID.randomUUID()).titulo("H1").orden(1)
                .estado(EstadoHito.PENDIENTE).build();
        Piso piso = buildPiso();
        HitoPiso hp = HitoPiso.builder().id(UUID.randomUUID()).hito(hito).piso(piso)
                .estado(EstadoHito.PENDIENTE).build();

        when(hitoPisoService.findByActivo(activoId)).thenReturn(List.of(hp));

        mockMvc.perform(get("/api/activos/" + activoId + "/hitos")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }

    @Test
    void getAvances_devuelveLista() throws Exception {
        UUID activoId = UUID.randomUUID();
        AvanceUnidadResponsePorcentajeDTO dto = new AvanceUnidadResponsePorcentajeDTO(
                UUID.randomUUID(), "H1", 1, EstadoHito.PENDIENTE, null, 0);
        when(hitoPisoService.obtenerAvancesPorActivo(activoId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/activos/" + activoId + "/avances")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }

    @Test
    void listarActivosPorProyecto_devuelvePaginado() throws Exception {
        UUID proyectoId = UUID.randomUUID();
        Activo activo = buildActivo();
        Page<com.llosa.backend.proyecto.dto.response.ActivoResponseDTO> page =
                new PageImpl<>(List.of(com.llosa.backend.proyecto.dto.response.ActivoResponseDTO.fromEntity(activo)));
        when(activoService.listarPorProyectoYEstado(eq(proyectoId), isNull(), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/activos/proyecto/" + proyectoId)
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerSeguimiento_devuelveDTO() throws Exception {
        UUID activoId = UUID.randomUUID();
        SeguimientoResponseDTO response = SeguimientoResponseDTO.builder()
                .stepper(List.of(PasoStepperDTO.builder().nombre("H1").estado("EN_PROGRESO").orden(1).build()))
                .faseActual(FaseActualDTO.builder()
                        .uuidHitoU(UUID.randomUUID()).titulo("H1")
                        .descripcion("Fase en progreso").porcentajeEtapa(0.0)
                        .fechaInicioFase(LocalDateTime.now()).build())
                .build();
        when(seguimientoService.obtenerSeguimiento(activoId)).thenReturn(response);

        mockMvc.perform(get("/api/activos/" + activoId + "/seguimiento")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }
}
