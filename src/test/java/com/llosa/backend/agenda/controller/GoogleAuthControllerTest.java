package com.llosa.backend.agenda.controller;

import com.llosa.backend.agenda.service.GoogleOAuthService;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoogleAuthController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class GoogleAuthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean GoogleOAuthService googleOAuthService;
    @MockitoBean UsuarioRepository usuarioRepository;
    @MockitoBean FirebaseConfig firebaseConfig;

    private static FirebaseAuthenticationToken authToken() {
        return new FirebaseAuthenticationToken("test-uid", "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private Usuario buildUsuario() {
        Usuario u = new Usuario();
        u.setId(1);
        u.setFirebaseUuid("test-uid");
        return u;
    }

    @Test
    void obtenerUrlAutorizacion_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/auth/google/url"))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerUrlAutorizacion_autenticado_devuelveUrl() throws Exception {
        Usuario usuario = buildUsuario();
        when(usuarioRepository.findByFirebaseUuid("test-uid")).thenReturn(Optional.of(usuario));
        when(googleOAuthService.generarUrlAutorizacion(1)).thenReturn("https://accounts.google.com/auth?code=abc");

        mockMvc.perform(get("/api/auth/google/url")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://accounts.google.com/auth?code=abc"));
    }

    @Test
    void obtenerUrlAutorizacion_usuarioNoEncontrado_devuelve404() throws Exception {
        when(usuarioRepository.findByFirebaseUuid("test-uid")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/google/url")
                        .with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void callback_sinError_devuelveMensajeExito() throws Exception {
        doNothing().when(googleOAuthService).procesarCallback("auth-code", "state-1");

        mockMvc.perform(get("/api/auth/google/callback")
                        .param("code", "auth-code")
                        .param("state", "state-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("conectado exitosamente")));
    }

    @Test
    void callback_conError_devuelveMensajeCancelacion() throws Exception {
        mockMvc.perform(get("/api/auth/google/callback")
                        .param("error", "access_denied"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cancelada")));

        verify(googleOAuthService, never()).procesarCallback(any(), any());
    }

    @Test
    void desconectar_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(delete("/api/auth/google")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void desconectar_autenticado_devuelve204() throws Exception {
        Usuario usuario = buildUsuario();
        when(usuarioRepository.findByFirebaseUuid("test-uid")).thenReturn(Optional.of(usuario));
        doNothing().when(googleOAuthService).desconectar(1);

        mockMvc.perform(delete("/api/auth/google")
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void desconectar_usuarioNoEncontrado_devuelve404() throws Exception {
        when(usuarioRepository.findByFirebaseUuid("test-uid")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/auth/google")
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
