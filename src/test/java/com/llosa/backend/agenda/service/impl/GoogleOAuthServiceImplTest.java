package com.llosa.backend.agenda.service.impl;

import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private GoogleOAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GoogleOAuthServiceImpl(usuarioRepository);
        ReflectionTestUtils.setField(service, "clientId", "dummy-client-id");
        ReflectionTestUtils.setField(service, "clientSecret", "dummy-client-secret");
        ReflectionTestUtils.setField(service, "redirectUri", "http://localhost:8080/api/auth/google/callback");
    }

    // ── generarUrlAutorizacion ───────────────────────────────────────────────

    @Test
    void generarUrlAutorizacion_construyeUrlConParametrosEsperados() {
        String url = service.generarUrlAutorizacion(42);

        assertThat(url).contains("client_id=dummy-client-id");
        assertThat(url).contains("state=42");
        assertThat(url).contains("prompt=consent");
    }

    // ── procesarCallback ─────────────────────────────────────────────────────

    @Test
    void procesarCallback_stateInvalido_lanzaIllegalArgumentException() {
        assertThatThrownBy(() -> service.procesarCallback("any-code", "no-es-un-numero"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parámetro 'state' inválido");

        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void procesarCallback_usuarioNoEncontrado_lanzaRecursoNoEncontradoException() {
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.procesarCallback("any-code", "99"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void procesarCallback_usuarioEncontrado_fallaIntercambioTokenSinRed() {
        Usuario gestor = new Usuario();
        gestor.setId(99);
        when(usuarioRepository.findById(99)).thenReturn(Optional.of(gestor));

        // Con usuario válido entra al try: buildFlow() + newTokenRequest(code).execute()
        // intenta contactar a Google y falla sin red, cayendo en el catch que relanza
        // IllegalStateException. Cubre las líneas del flujo de intercambio de token.
        assertThatThrownBy(() -> service.procesarCallback("fake-auth-code", "99"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo completar la autorización");

        verify(usuarioRepository, never()).save(any());
    }

    // ── desconectar ──────────────────────────────────────────────────────────

    @Test
    void desconectar_usuarioNoEncontrado_lanzaRecursoNoEncontradoException() {
        when(usuarioRepository.findById(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desconectar(7))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desconectar_usuarioEncontrado_limpiaRefreshTokenYDesactiva() {
        Usuario gestor = new Usuario();
        gestor.setId(7);
        gestor.setGoogleRefreshToken("old-refresh-token");
        gestor.setGoogleCalendarConectado(true);

        when(usuarioRepository.findById(7)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.desconectar(7);

        assertThat(gestor.getGoogleRefreshToken()).isNull();
        assertThat(gestor.getGoogleCalendarConectado()).isFalse();
        verify(usuarioRepository).save(gestor);
    }
}
