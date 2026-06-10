package com.llosa.backend.proyecto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.UsuarioActivoService;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import com.llosa.backend.seguridad.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioActivoController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class UsuarioActivoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UsuarioActivoService usuarioActivoService;
    @MockitoBean ActivoService activoService;
    @MockitoBean UsuarioService usuarioService;
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

    private SecurityContext contextWithFullAuth() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new FirebaseAuthenticationToken("test-uid", "test@test.com",
                List.of(new SimpleGrantedAuthority("CONTRATO_VER"),
                        new SimpleGrantedAuthority("CONTRATO_EDITAR"))));
        return ctx;
    }

    @Test
    void obtenerMisActivos_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/expedientes/mis-activos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerMisActivos_autenticado_devuelveLista() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setFirebaseUuid("test-uid");

        Activo activo = buildActivo();
        UsuarioActivo ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID()).activo(activo).build();

        when(usuarioService.findByFirebaseUuid("test-uid")).thenReturn(usuario);
        when(usuarioActivoService.findByUsuario(1)).thenReturn(List.of(ua));

        mockMvc.perform(get("/api/expedientes/mis-activos")
                        .with(securityContext(contextWithFullAuth())))
                .andExpect(status().isOk());
    }

    @Test
    void asignarActivoAUsuario_valido_devuelve200() throws Exception {
        UUID idActivo = UUID.randomUUID();
        doNothing().when(usuarioActivoService).asignarActivo(any(AsignarActivoDTO.class));

        AsignarActivoDTO dto = new AsignarActivoDTO(List.of(1), idActivo,
                "Crédito Hipotecario", "Separación", "Pendiente", null);

        mockMvc.perform(post("/api/expedientes/asignar")
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void verContratoActivoUsuario_encontrado_devuelve200() throws Exception {
        UUID activoId = UUID.randomUUID();
        Activo activo = buildActivo();
        UsuarioActivo ua = UsuarioActivo.builder()
                .uuidUsuarioActivo(UUID.randomUUID())
                .activo(activo)
                .clientes(List.of())
                .faseComercial("Separación")
                .estadoTramiteLegal("Pendiente")
                .build();

        when(usuarioActivoService.findByActivo(activoId)).thenReturn(Optional.of(ua));

        mockMvc.perform(get("/api/expedientes/" + activoId + "/contrato")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk());
    }

    @Test
    void verContratoActivoUsuario_noEncontrado_devuelve404() throws Exception {
        UUID activoId = UUID.randomUUID();
        when(usuarioActivoService.findByActivo(activoId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/expedientes/" + activoId + "/contrato")
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void desvincularActivoAUsuario_devuelve204() throws Exception {
        UUID uaId = UUID.randomUUID();
        doNothing().when(usuarioActivoService).deleteById(uaId);

        mockMvc.perform(delete("/api/expedientes/delete/" + uaId)
                        .with(authentication(TestData.proyectoAuthToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
