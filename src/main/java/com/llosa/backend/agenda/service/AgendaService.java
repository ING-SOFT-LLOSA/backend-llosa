package com.llosa.backend.agenda.service;

import com.llosa.backend.agenda.dto.request.*;
import com.llosa.backend.agenda.dto.response.CitaResponse;
import com.llosa.backend.agenda.dto.response.DisponibilidadResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AgendaService {

    // ── Gestión desde el Portal Empresa ──────────────────────────────────────

    /** Crea una cita, intenta sincronizar con Google Calendar si aplica. */
    CitaResponse crearCita(String gestorFirebaseUid, CrearCitaRequest request);

    /** Actualiza datos de la cita y re-sincroniza con Google Calendar si aplica. */
    CitaResponse actualizarCita(UUID citaId, ActualizarCitaRequest request);

    /** Cancela la cita y elimina el evento de Google Calendar si existía. */
    CitaResponse cancelarCita(UUID citaId, String motivo);

    /** Selecciona el bloque propuesto por el cliente y reprograma la cita. */
    CitaResponse seleccionarBloqueDisponibilidad(UUID citaId, SeleccionarBloqueRequest request);

    /** Lista todas las citas de un activo (para la vista del backoffice). */
    List<CitaResponse> listarCitasPorActivo(UUID activoId);

    /** Lista citas de un gestor en un rango de fechas (calendario backoffice). */
    List<CitaResponse> listarCitasGestor(String gestorFirebaseUid,
                                         LocalDateTime inicio,
                                         LocalDateTime fin);

    /** Lista TODAS las citas del sistema (solo ADMIN). */
    List<CitaResponse> listarTodasLasCitas();

    // ── Gestión desde el Portal Cliente ──────────────────────────────────────

    /**
     * Lista las próximas citas activas del cliente autenticado.
     * Funciona tanto si el cliente usa Google Calendar como si no.
     */
    List<CitaResponse> listarCitasCliente(String clienteFirebaseUid);

    /** Lista SOLO las próximas citas (futuras + activas) del cliente. */
    List<CitaResponse> listarProximasCitasCliente(String clienteFirebaseUid);

    /** El cliente confirma o declina una cita. */
    CitaResponse responderCita(UUID citaId, String clienteFirebaseUid, RespuestaClienteRequest request);

    /**
     * El cliente propone bloques de disponibilidad para reprogramar.
     * Solo permitido si permiteReprogramacion = true.
     */
    List<DisponibilidadResponse> proponerDisponibilidad(UUID citaId,
                                                        String clienteFirebaseUid,
                                                        DisponibilidadRequest request);

    // ── Worker de reintentos ──────────────────────────────────────────────────

    /**
     * Reintenta sincronizar las citas que quedaron en estado PENDIENTE
     * por fallo temporal de Google Calendar API.
     * Invocado por el scheduler @Scheduled.
     */
    void reintentarSincronizacionesPendientes();
}
