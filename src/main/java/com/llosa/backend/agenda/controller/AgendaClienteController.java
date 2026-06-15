package com.llosa.backend.agenda.controller;

import com.llosa.backend.agenda.dto.request.DisponibilidadRequest;
import com.llosa.backend.agenda.dto.request.RespuestaClienteRequest;
import com.llosa.backend.agenda.dto.response.CitaResponse;
import com.llosa.backend.agenda.dto.response.DisponibilidadResponse;
import com.llosa.backend.agenda.service.AgendaService;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints del módulo de agenda para el Portal Cliente.
 *
 * Todos los endpoints son de uso exclusivo del cliente autenticado.
 * La privacidad se garantiza en la capa de servicio: el UID del token
 * Firebase se usa para resolver al cliente y verificar pertenencia.
 *
 * Funciona correctamente independientemente de si el cliente tiene
 * cuenta Google o no:
 *   - Con Google: las citas también viven en Google Calendar.
 *   - Sin Google: las citas solo están en el backend y se consultan aquí.
 */
@RestController
@RequestMapping("/api/agenda/cliente")
@RequiredArgsConstructor
public class AgendaClienteController {

    private final AgendaService agendaService;

    /**
     * GET /api/agenda/cliente/citas
     * Retorna TODAS las citas del cliente (históricas + futuras).
     * El frontend puede filtrar por estado para mostrar el historial.
     */
    @PreAuthorize("hasAuthority('AGENDA_VER')")
    @GetMapping("/citas")
    public ResponseEntity<List<CitaResponse>> listarMisCitas(Authentication auth) {
        return ResponseEntity.ok(
                agendaService.listarCitasCliente(extraerUid(auth))
        );
    }

    /**
     * GET /api/agenda/cliente/citas/proximas
     * Retorna solo las PRÓXIMAS citas activas (PROGRAMADA o CONFIRMADA, en el futuro).
     * Este es el endpoint principal que usa el widget de "Agenda & Citas" del dashboard.
     *
     * Es el fallback clave para clientes sin Google Calendar:
     * el frontend consume este endpoint en lugar de Google Calendar API.
     */
    @PreAuthorize("hasAuthority('AGENDA_VER')")
    @GetMapping("/citas/proximas")
    public ResponseEntity<List<CitaResponse>> listarProximasCitas(Authentication auth) {
        return ResponseEntity.ok(
                agendaService.listarProximasCitasCliente(extraerUid(auth))
        );
    }

    /**
     * PATCH /api/agenda/cliente/citas/{id}/respuesta
     * El cliente confirma (true) o declina (false) una cita.
     * Si la cita está sincronizada con Google Calendar, actualiza el RSVP allí también.
     * Si no tiene Google, actualiza solo el estado en la BD.
     */
    @PreAuthorize("hasAuthority('AGENDA_VER')")
    @PatchMapping("/citas/{id}/respuesta")
    public ResponseEntity<CitaResponse> responderCita(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody RespuestaClienteRequest request) {

        return ResponseEntity.ok(
                agendaService.responderCita(id, extraerUid(auth), request)
        );
    }

    /**
     * POST /api/agenda/cliente/citas/{id}/disponibilidad
     * El cliente propone bloques horarios alternativos para reprogramar.
     * Solo disponible si la cita tiene permiteReprogramacion = true.
     *
     * La fecha del evento en Google Calendar NO cambia aquí;
     * el gestor debe confirmar el bloque desde el Portal Empresa.
     */
    @PreAuthorize("hasAuthority('AGENDA_VER')")
    @PostMapping("/citas/{id}/disponibilidad")
    public ResponseEntity<List<DisponibilidadResponse>> proponerDisponibilidad(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody DisponibilidadRequest request) {

        return ResponseEntity.ok(
                agendaService.proponerDisponibilidad(id, extraerUid(auth), request)
        );
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private String extraerUid(Authentication auth) {
        if (auth instanceof FirebaseAuthenticationToken token) {
            return (String) token.getPrincipal();
        }
        throw new IllegalStateException("Token de autenticación inválido");
    }
}
