package com.llosa.backend.agenda.service.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.llosa.backend.agenda.entity.Cita;
import com.llosa.backend.agenda.service.GoogleCalendarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
// Nota: EventAttendee ya no se usa en crearEvento/actualizarEvento.
// Se mantiene la importación por si se reactiva con OAuth o DWD en el futuro.

/**
 * Implementación real de la integración con Google Calendar API.
 *
 * Usa la cuenta de servicio (service account) configurada en Firebase/GCP
 * para actuar como anfitrión institucional. El evento se crea en el
 * calendario del service account y se invita al cliente como asistente.
 *
 * Si el cliente NO tiene cuenta Google (clienteUsaGoogle = false),
 * este servicio NO es llamado; la cita solo vive en la BD.
 */
@Slf4j
@Service
public class GoogleCalendarServiceImpl implements GoogleCalendarService {

    private static final String CALENDAR_ID = "primary";
    private static final String ZONA_HORARIA = "America/Lima";
    private static final String APPLICATION_NAME = "Llosa Edificaciones";

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    /**
     * Construye el cliente autenticado de Google Calendar usando la
     * misma service account key que usa Firebase Admin SDK.
     */
    private Calendar buildCalendarClient() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(serviceAccountPath))
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName(APPLICATION_NAME).build();
    }

    @Override
    public Optional<String> crearEvento(Cita cita) {
        try {
            Calendar service = buildCalendarClient();

            Event event = buildEvent(cita);

            // NOTA: Las Service Accounts no tienen permiso para enviar invitaciones
            // a asistentes externos (requeriría Domain-Wide Delegation en Google Workspace).
            // El evento se crea solo en el calendario institucional (service account).
            // La notificación al cliente se gestiona por email desde la propia app.
            // setSendUpdates("none") evita el 403 "forbiddenForServiceAccounts".
            Event createdEvent = service.events()
                    .insert(CALENDAR_ID, event)
                    .setSendUpdates("none")
                    .execute();

            log.info("[Google Calendar] Evento creado. ID={}, Cita={}",
                    createdEvent.getId(), cita.getId());
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
        try {
            Calendar service = buildCalendarClient();

            Event event = buildEvent(cita);

            // Mismo criterio que crearEvento: la service account no puede
            // invitar asistentes. setSendUpdates("none") previene el 403.
            service.events()
                    .update(CALENDAR_ID, cita.getGoogleEventId(), event)
                    .setSendUpdates("none")
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
        if (googleEventId == null) return false;
        try {
            Calendar service = buildCalendarClient();
            service.events().delete(CALENDAR_ID, googleEventId).execute();
            log.info("[Google Calendar] Evento eliminado. ID={}", googleEventId);
            return true;
        } catch (Exception e) {
            log.error("[Google Calendar] Error al eliminar evento {}: {}",
                    googleEventId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean actualizarRsvpInvitado(String googleEventId,
                                          String emailCliente,
                                          boolean confirmado) {
        if (googleEventId == null) return false;
        try {
            Calendar service = buildCalendarClient();

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