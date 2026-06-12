package com.llosa.backend.agenda.service;

import com.llosa.backend.agenda.entity.Cita;

import java.util.Optional;

/**
 * Abstracción de la integración con Google Calendar API.
 *
 * Todas las operaciones retornan Optional para manejar de forma
 * explícita los fallos de red sin lanzar excepciones no controladas,
 * permitiendo que el servicio principal aplique el fallback a BD.
 */
public interface GoogleCalendarService {

    /**
     * Crea un evento en el calendario institucional de Llosa Edificaciones
     * e invita al cliente como asistente.
     *
     * @return el googleEventId si la creación fue exitosa, vacío si falló.
     */
    Optional<String> crearEvento(Cita cita);

    /**
     * Actualiza un evento existente (fecha, título, descripción, ubicación).
     *
     * @return true si la actualización fue exitosa.
     */
    boolean actualizarEvento(Cita cita);

    /**
     * Elimina/cancela el evento del calendario.
     *
     * @return true si la eliminación fue exitosa.
     */
    boolean eliminarEvento(String googleEventId);

    /**
     * Actualiza únicamente el estado RSVP del invitado (Confirmado / Declinado).
     * Usado cuando el cliente responde desde el Portal Cliente.
     *
     * @return true si la actualización fue exitosa.
     */
    boolean actualizarRsvpInvitado(String googleEventId, String emailCliente, boolean confirmado);
}
