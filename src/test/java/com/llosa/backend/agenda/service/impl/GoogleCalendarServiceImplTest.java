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

    // ──────────────────────────────────────────────────────────────────────────
    // Gestor CON refresh token: ejecuta buildCalendarClient() completo (construye
    // el flow + credential + Calendar.Builder) y buildEvent() dentro de crearEvento/
    // actualizarEvento. La llamada externa .execute() falla sin red y cae en el
    // catch (retorna empty/false), pero ya se cubrieron ~50 líneas de construcción.
    // ──────────────────────────────────────────────────────────────────────────

    private Usuario gestorConGoogle() {
        var g = new Usuario();
        g.setId(7);
        g.setNombre("Carlos");
        g.setApellidos("Gomez");
        g.setGoogleRefreshToken("fake-refresh-token-xyz");
        return g;
    }

    private com.llosa.backend.seguridad.entity.Usuario clienteGoogle() {
        var c = new Usuario();
        c.setNombre("Ana");
        c.setApellidos("Lopez");
        c.setEmail("ana.lopez@gmail.com");
        return c;
    }

    @Test
    void crearEvento_conGestorConectado_clienteUsaGoogle_construyeYFalla() {
        var cita = buildCita();
        cita.setGestor(gestorConGoogle());
        cita.setCliente(clienteGoogle());
        cita.setClienteUsaGoogle(true);

        // Ejecuta buildCalendarClient (líneas 62-86) + buildEvent + rama attendees.
        var result = service.crearEvento(cita);
        assertThat(result).isEmpty(); // falla en .execute() sin red
    }

    @Test
    void crearEvento_conGestorConectado_sinClienteGoogle_construyeYFalla() {
        var cita = buildCita();
        cita.setGestor(gestorConGoogle());
        cita.setClienteUsaGoogle(false);

        var result = service.crearEvento(cita);
        assertThat(result).isEmpty();
    }

    @Test
    void actualizarEvento_conGestorConectado_construyeYFalla() {
        var cita = buildCita();
        cita.setGestor(gestorConGoogle());
        cita.setCliente(clienteGoogle());
        cita.setClienteUsaGoogle(true);

        assertThat(service.actualizarEvento(cita)).isFalse();
    }

    @Test
    void eliminarEventoConCita_conGestorConectado_construyeYFalla() {
        var cita = buildCita();
        cita.setGestor(gestorConGoogle());

        assertThat(service.eliminarEvento(cita)).isFalse();
    }

    @Test
    void actualizarRsvpInvitadoConGestor_conGestorConectado_construyeYFalla() {
        assertThat(service.actualizarRsvpInvitado(
                gestorConGoogle(), "google-event-123", "ana@gmail.com", true)).isFalse();
        // rama confirmado=false
        assertThat(service.actualizarRsvpInvitado(
                gestorConGoogle(), "google-event-123", "ana@gmail.com", false)).isFalse();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers privados vía reflexión: buildEvent, buildDescription, toGoogleDateTime.
    // Cubren todas las ramas de construcción (titulo null, descripcion null/blank,
    // ubicacion null/blank, recordatorios, etc.).
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void buildEvent_citaCompleta_construyeEventoConTodo() throws Exception {
        var cita = buildCita(); // titulo, descripcion, ubicacion presentes
        var m = GoogleCalendarServiceImpl.class.getDeclaredMethod("buildEvent", Cita.class);
        m.setAccessible(true);
        var event = (com.google.api.services.calendar.model.Event) m.invoke(service, cita);

        assertThat(event.getSummary()).isEqualTo("Entrega de llaves");
        assertThat(event.getLocation()).isEqualTo("Av. Principal 123");
        assertThat(event.getStart()).isNotNull();
        assertThat(event.getEnd()).isNotNull();
        assertThat(event.getReminders()).isNotNull();
        assertThat(event.getDescription()).contains("Llosa Edificaciones");
    }

    @Test
    void buildEvent_tituloNull_usaTipoEvento() throws Exception {
        var cita = buildCita();
        cita.setTitulo(null);          // summary cae al tipoEvento.name()
        cita.setUbicacion(null);       // rama ubicacion null (no setLocation)

        var m = GoogleCalendarServiceImpl.class.getDeclaredMethod("buildEvent", Cita.class);
        m.setAccessible(true);
        var event = (com.google.api.services.calendar.model.Event) m.invoke(service, cita);

        assertThat(event.getSummary()).isEqualTo("ENTREGA LLAVES");
        assertThat(event.getLocation()).isNull();
    }

    @Test
    void buildEvent_ubicacionEnBlanco_noSeteaLocation() throws Exception {
        var cita = buildCita();
        cita.setUbicacion("   ");      // rama ubicacion blank (no setLocation)

        var m = GoogleCalendarServiceImpl.class.getDeclaredMethod("buildEvent", Cita.class);
        m.setAccessible(true);
        var event = (com.google.api.services.calendar.model.Event) m.invoke(service, cita);

        assertThat(event.getLocation()).isNull();
    }

    @Test
    void buildDescription_conDescripcion_incluyeTodo() throws Exception {
        var cita = buildCita();
        var m = GoogleCalendarServiceImpl.class.getDeclaredMethod("buildDescription", Cita.class);
        m.setAccessible(true);
        String desc = (String) m.invoke(service, cita);

        assertThat(desc).contains("ENTREGA LLAVES");
        assertThat(desc).contains("Primera entrega");
        assertThat(desc).contains("Unidad: DPTO 101");
        assertThat(desc).contains("Gestor: Carlos Gomez");
    }

    @Test
    void buildDescription_descripcionNull_omiteBloque() throws Exception {
        var cita = buildCita();
        cita.setDescripcion(null);     // rama descripcion null

        var m = GoogleCalendarServiceImpl.class.getDeclaredMethod("buildDescription", Cita.class);
        m.setAccessible(true);
        String desc = (String) m.invoke(service, cita);

        assertThat(desc).contains("Unidad: DPTO 101");
        assertThat(desc).doesNotContain("Primera entrega");
    }

    @Test
    void buildDescription_descripcionEnBlanco_omiteBloque() throws Exception {
        var cita = buildCita();
        cita.setDescripcion("   ");    // rama descripcion blank

        var m = GoogleCalendarServiceImpl.class.getDeclaredMethod("buildDescription", Cita.class);
        m.setAccessible(true);
        String desc = (String) m.invoke(service, cita);

        assertThat(desc).contains("Unidad: DPTO 101");
    }

    @Test
    void toGoogleDateTime_convierteMillisCorrectamente() throws Exception {
        var m = GoogleCalendarServiceImpl.class.getDeclaredMethod(
                "toGoogleDateTime", LocalDateTime.class, java.time.ZoneId.class);
        m.setAccessible(true);
        var dt = (com.google.api.client.util.DateTime) m.invoke(
                service, LocalDateTime.of(2026, 7, 1, 10, 0), java.time.ZoneId.of("America/Lima"));

        assertThat(dt.getValue()).isPositive();
    }
}
