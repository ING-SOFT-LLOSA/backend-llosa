package com.llosa.backend.agenda.service.impl;

import com.llosa.backend.agenda.entity.Cita;
import com.llosa.backend.agenda.enums.TipoEvento;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.seguridad.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceImplTest {

    private GoogleCalendarServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GoogleCalendarServiceImpl();
    }

    private Cita buildCita() {
        var gestor = new Usuario();
        gestor.setNombre("Carlos");
        gestor.setApellidos("Gomez");

        var activo = Activo.builder().id(UUID.randomUUID()).nro("DPTO 101").build();

        return Cita.builder()
                .id(UUID.randomUUID())
                .titulo("Entrega de llaves")
                .descripcion("Primera entrega")
                .ubicacion("Av. Principal 123")
                .tipoEvento(TipoEvento.ENTREGA_LLAVES)
                .fechaInicio(LocalDateTime.of(2026, 7, 1, 10, 0))
                .fechaFin(LocalDateTime.of(2026, 7, 1, 11, 0))
                .gestor(gestor)
                .activo(activo)
                .googleEventId("google-event-123")
                .build();
    }

    @Test
    void crearEvento_pathInvalido_retornaEmpty() {
        var cita = buildCita();
        cita.setGoogleEventId(null);

        var result = service.crearEvento(cita);

        assertThat(result).isEmpty();
    }

    @Test
    void actualizarEvento_googleEventIdNull_retornaFalse() {
        var cita = buildCita();
        cita.setGoogleEventId(null);

        assertThat(service.actualizarEvento(cita)).isFalse();
    }

    @Test
    void actualizarEvento_pathInvalido_retornaFalse() {
        var cita = buildCita();

        assertThat(service.actualizarEvento(cita)).isFalse();
    }

    @Test
    void eliminarEvento_googleEventIdNull_retornaFalse() {
        assertThat(service.eliminarEvento((String) null)).isFalse();
    }

    @Test
    void eliminarEvento_pathInvalido_retornaFalse() {
        assertThat(service.eliminarEvento("google-event-123")).isFalse();
    }

    @Test
    void actualizarRsvpInvitado_googleEventIdNull_retornaFalse() {
        assertThat(service.actualizarRsvpInvitado(null, "test@test.com", true)).isFalse();
    }

    @Test
    void actualizarRsvpInvitado_pathInvalido_retornaFalse() {
        assertThat(service.actualizarRsvpInvitado("google-event-123", "test@test.com", true)).isFalse();
    }

    // ── crearEvento: gestor null (rama no cubierta del log.warn) ─────────────

    @Test
    void crearEvento_sinGestor_retornaEmpty() {
        var cita = buildCita();
        cita.setGestor(null);

        var result = service.crearEvento(cita);

        assertThat(result).isEmpty();
    }

    // ── eliminarEvento(Cita) - sobrecarga con contexto del gestor ────────────

    @Test
    void eliminarEventoConCita_googleEventIdNull_retornaFalse() {
        var cita = buildCita();
        cita.setGoogleEventId(null);

        assertThat(service.eliminarEvento(cita)).isFalse();
    }

    @Test
    void eliminarEventoConCita_gestorSinGoogleConectado_retornaFalse() {
        var cita = buildCita();

        assertThat(service.eliminarEvento(cita)).isFalse();
    }

    @Test
    void eliminarEventoConCita_sinGestor_retornaFalse() {
        var cita = buildCita();
        cita.setGestor(null);

        assertThat(service.eliminarEvento(cita)).isFalse();
    }

    // ── actualizarRsvpInvitado(Usuario, ...) - sobrecarga con contexto ───────

    @Test
    void actualizarRsvpInvitadoConGestor_googleEventIdNull_retornaFalse() {
        var gestor = new Usuario();
        gestor.setNombre("Carlos");

        assertThat(service.actualizarRsvpInvitado(gestor, null, "test@test.com", true)).isFalse();
    }

    @Test
    void actualizarRsvpInvitadoConGestor_gestorSinGoogleConectado_retornaFalse() {
        var gestor = new Usuario();
        gestor.setNombre("Carlos");

        assertThat(service.actualizarRsvpInvitado(gestor, "google-event-123", "test@test.com", true)).isFalse();
    }

    @Test
    void actualizarRsvpInvitadoConGestor_sinGestor_retornaFalse() {
        assertThat(service.actualizarRsvpInvitado(null, "google-event-123", "test@test.com", true)).isFalse();
    }
}
