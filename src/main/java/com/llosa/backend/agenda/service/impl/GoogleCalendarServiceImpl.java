package com.llosa.backend.agenda.service.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.llosa.backend.agenda.entity.Cita;
import com.llosa.backend.agenda.service.GoogleCalendarService;
import com.llosa.backend.seguridad.entity.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Implementación real de la integración con Google Calendar API.
 *
 * A diferencia de la versión anterior (que usaba una service account),
 * esta implementación crea el evento usando el token OAuth del GESTOR
 * (obtenido cuando el gestor conecta su cuenta vía /api/auth/google).
 *
 * Esto permite invitar al cliente como asistente real (attendee),
 * algo que una service account no puede hacer sin Domain-Wide Delegation.
 *
 * Si el gestor NO ha conectado su Google Calendar (googleCalendarConectado = false,
 * o googleRefreshToken = null), las operaciones fallan de forma controlada
 * (Optional.empty() / false) y AgendaServiceImpl debe manejar el fallback a BD.
 */
@Slf4j
@Service
public class GoogleCalendarServiceImpl implements GoogleCalendarService {

    private static final String CALENDAR_ID = "primary";
    private static final String ZONA_HORARIA = "America/Lima";
    private static final String APPLICATION_NAME = "Llosa Edificaciones";

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    /**
     * Construye el cliente autenticado de Google Calendar usando el
     * refresh token guardado del gestor. Si el gestor no conectó su cuenta,
     * retorna empty y el caller debe manejar el fallback.
     */
    private Optional<Calendar> buildCalendarClient(Usuario gestor) {
        if (gestor == null || gestor.getGoogleRefreshToken() == null) {
            log.warn("[Google Calendar] Gestor {} no tiene Google Calendar conectado.",
                    gestor != null ? gestor.getId() : "null");
            return Optional.empty();
        }

        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientId,
                    clientSecret,
                    List.of(CalendarScopes.CALENDAR_EVENTS))
                    .setAccessType("offline")
                    .build();

            // Reconstruye el Credential a partir del refresh token guardado.
            // La librería se encarga de pedir un nuevo access_token a Google
            // automáticamente usando este refresh token.
            Credential credential = flow.createAndStoreCredential(
                    new com.google.api.client.auth.oauth2.TokenResponse()
                            .setRefreshToken(gestor.getGoogleRefreshToken()),
                    gestor.getId().toString());

            Calendar service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            return Optional.of(service);

        } catch (Exception e) {
            log.error("[Google Calendar] Error al construir cliente para gestor {}: {}",
                    gestor.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> crearEvento(Cita cita) {
        Optional<Calendar> serviceOpt = buildCalendarClient(cita.getGestor());
        if (serviceOpt.isEmpty()) {
            return Optional.empty();
        }
        Calendar service = serviceOpt.get();

        try {
            Event event = buildEvent(cita);

            // Ahora SÍ podemos invitar al cliente: el evento se crea
            // en nombre del gestor (persona real), no de una service account.
            if (Boolean.TRUE.equals(cita.getClienteUsaGoogle())) {
                EventAttendee attendee = new EventAttendee()
                        .setEmail(cita.getCliente().getEmail())
                        .setDisplayName(cita.getCliente().getNombre()
                                + " " + cita.getCliente().getApellidos());
                event.setAttendees(List.of(attendee));
            }

            Event createdEvent = service.events()
                    .insert(CALENDAR_ID, event)
                    .setSendUpdates(
                            Boolean.TRUE.equals(cita.getClienteUsaGoogle()) ? "all" : "none")
                    .execute();

            log.info("[Google Calendar] Evento creado. ID={}, Cita={}, Gestor={}",
                    createdEvent.getId(), cita.getId(), cita.getGestor().getId());
            return Optional.of(createdEvent.getId());

        } catch (Exception e) {
            log.error("[Google Calendar] Error al crear evento para cita {}: {}",
                    cita.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean actualizarEvento(Cita cita) {
        if (cita.getGoogleEventId() == null) return false;

        Optional<Calendar> serviceOpt = buildCalendarClient(cita.getGestor());
        if (serviceOpt.isEmpty()) {
            return false;
        }
        Calendar service = serviceOpt.get();

        try {
            Event event = buildEvent(cita);

            if (Boolean.TRUE.equals(cita.getClienteUsaGoogle())) {
                EventAttendee attendee = new EventAttendee()
                        .setEmail(cita.getCliente().getEmail());
                event.setAttendees(List.of(attendee));
            }

            service.events()
                    .update(CALENDAR_ID, cita.getGoogleEventId(), event)
                    .setSendUpdates(
                            Boolean.TRUE.equals(cita.getClienteUsaGoogle()) ? "all" : "none")
                    .execute();

            log.info("[Google Calendar] Evento actualizado. ID={}", cita.getGoogleEventId());
            return true;

        } catch (Exception e) {
            log.error("[Google Calendar] Error al actualizar evento {}: {}",
                    cita.getGoogleEventId(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean eliminarEvento(String googleEventId) {
        // NOTA: este método no recibe la Cita completa, solo el id del evento,
        // por lo que no tenemos acceso directo al gestor desde aquí.
        // Si esto falla, revisa el caller (AgendaServiceImpl) para que pase
        // la Cita completa en vez de solo el id, igual que los demás métodos.
        log.warn("[Google Calendar] eliminarEvento(String) ya no tiene contexto del gestor. " +
                "Considera cambiar la firma a eliminarEvento(Cita cita).");
        return false;
    }

    /**
     * Variante que sí recibe la Cita completa, necesaria para resolver
     * las credenciales del gestor. Úsala desde AgendaServiceImpl en vez
     * de eliminarEvento(String).
     */
    public boolean eliminarEvento(Cita cita) {
        if (cita.getGoogleEventId() == null) return false;

        Optional<Calendar> serviceOpt = buildCalendarClient(cita.getGestor());
        if (serviceOpt.isEmpty()) {
            return false;
        }

        try {
            serviceOpt.get().events().delete(CALENDAR_ID, cita.getGoogleEventId()).execute();
            log.info("[Google Calendar] Evento eliminado. ID={}", cita.getGoogleEventId());
            return true;
        } catch (Exception e) {
            log.error("[Google Calendar] Error al eliminar evento {}: {}",
                    cita.getGoogleEventId(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean actualizarRsvpInvitado(String googleEventId,
                                          String emailCliente,
                                          boolean confirmado) {
        log.warn("[Google Calendar] actualizarRsvpInvitado(String,...) ya no tiene contexto " +
                "del gestor. Considera agregar el parámetro Usuario gestor o Cita.");
        return false;
    }

    /**
     * Variante con contexto del gestor, necesaria para resolver credenciales.
     */
    public boolean actualizarRsvpInvitado(Usuario gestor, String googleEventId,
                                          String emailCliente, boolean confirmado) {
        if (googleEventId == null) return false;

        Optional<Calendar> serviceOpt = buildCalendarClient(gestor);
        if (serviceOpt.isEmpty()) {
            return false;
        }
        Calendar service = serviceOpt.get();

        try {
            Event event = service.events().get(CALENDAR_ID, googleEventId).execute();
            if (event.getAttendees() == null) return false;

            String responseStatus = confirmado ? "accepted" : "declined";
            event.getAttendees().stream()
                    .filter(a -> emailCliente.equalsIgnoreCase(a.getEmail()))
                    .forEach(a -> a.setResponseStatus(responseStatus));

            service.events()
                    .update(CALENDAR_ID, googleEventId, event)
                    .setSendUpdates("none")
                    .execute();

            log.info("[Google Calendar] RSVP actualizado. EventID={}, Email={}, Status={}",
                    googleEventId, emailCliente, responseStatus);
            return true;

        } catch (Exception e) {
            log.error("[Google Calendar] Error al actualizar RSVP {}: {}",
                    googleEventId, e.getMessage());
            return false;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Event buildEvent(Cita cita) {
        ZoneId limaZone = ZoneId.of(ZONA_HORARIA);

        String summary = cita.getTitulo() != null
                ? cita.getTitulo()
                : cita.getTipoEvento().name().replace("_", " ");

        String description = buildDescription(cita);

        EventDateTime start = new EventDateTime()
                .setDateTime(toGoogleDateTime(cita.getFechaInicio(), limaZone))
                .setTimeZone(ZONA_HORARIA);

        EventDateTime end = new EventDateTime()
                .setDateTime(toGoogleDateTime(cita.getFechaFin(), limaZone))
                .setTimeZone(ZONA_HORARIA);

        Event event = new Event()
                .setSummary(summary)
                .setDescription(description)
                .setStart(start)
                .setEnd(end);

        if (cita.getUbicacion() != null && !cita.getUbicacion().isBlank()) {
            event.setLocation(cita.getUbicacion());
        }

        // Recordatorio 24h y 1h antes
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(List.of(
                        new EventReminder().setMethod("email").setMinutes(24 * 60),
                        new EventReminder().setMethod("popup").setMinutes(60)
                ));
        event.setReminders(reminders);

        return event;
    }

    private String buildDescription(Cita cita) {
        StringBuilder sb = new StringBuilder();
        sb.append("Llosa Edificaciones - ").append(cita.getTipoEvento().name()
                .replace("_", " ")).append("\n\n");

        if (cita.getDescripcion() != null && !cita.getDescripcion().isBlank()) {
            sb.append(cita.getDescripcion()).append("\n\n");
        }

        sb.append("Unidad: ").append(cita.getActivo().getNro()).append("\n");
        sb.append("Gestor: ").append(cita.getGestor().getNombre())
                .append(" ").append(cita.getGestor().getApellidos()).append("\n");

        return sb.toString();
    }

    private com.google.api.client.util.DateTime toGoogleDateTime(
            java.time.LocalDateTime ldt, ZoneId zone) {
        long millis = ldt.atZone(zone).toInstant().toEpochMilli();
        return new com.google.api.client.util.DateTime(millis);
    }
}