package com.llosa.backend.agenda.controller;

import com.llosa.backend.agenda.dto.request.ActualizarCitaRequest;
import com.llosa.backend.agenda.dto.request.CrearCitaRequest;
import com.llosa.backend.agenda.dto.request.SeleccionarBloqueRequest;
import com.llosa.backend.agenda.dto.response.CitaResponse;
import com.llosa.backend.agenda.service.AgendaService;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints del módulo de agenda para el Portal Empresa (backoffice).
 *
 * Permisos requeridos:
 *   - AGENDA_VER:    listar y consultar citas.
 *   - AGENDA_CREAR:  crear nuevas citas.
 *   - AGENDA_EDITAR: actualizar, cancelar y gestionar disponibilidad.
 */
@RestController
@RequestMapping("/api/agenda/empresa")
@RequiredArgsConstructor
public class AgendaEmpresaController {

    private final AgendaService agendaService;

    /**
     * POST /api/agenda/empresa/citas
     * Crea una nueva cita. Intenta sincronizar con Google Calendar si
     * clienteUsaGoogle = true; en caso contrario, persiste solo en BD.
     */
    @PreAuthorize("hasAnyAuthority('AGENDA_CREAR', 'ADMIN_TOTAL')")
    @PostMapping("/citas")
    public ResponseEntity<CitaResponse> crearCita(
            Authentication auth,
            @Valid @RequestBody CrearCitaRequest request) {

        String gestorUid = extraerUid(auth);
        CitaResponse response = agendaService.crearCita(gestorUid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/agenda/empresa/citas/{id}
     * Actualiza datos de la cita y re-sincroniza con Google Calendar si aplica.
     */
    @PreAuthorize("hasAnyAuthority('AGENDA_EDITAR', 'ADMIN_TOTAL')")
    @PutMapping("/citas/{id}")
    public ResponseEntity<CitaResponse> actualizarCita(
            @PathVariable UUID id,
            @RequestBody ActualizarCitaRequest request) {

        return ResponseEntity.ok(agendaService.actualizarCita(id, request));
    }

    /**
     * DELETE /api/agenda/empresa/citas/{id}
     * Cancela la cita y elimina el evento de Google Calendar si existía.
     */
    @PreAuthorize("hasAnyAuthority('AGENDA_EDITAR', 'ADMIN_TOTAL')")
    @DeleteMapping("/citas/{id}")
    public ResponseEntity<CitaResponse> cancelarCita(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "Cancelado por la empresa") String motivo) {

        return ResponseEntity.ok(agendaService.cancelarCita(id, motivo));
    }

    /**
     * PATCH /api/agenda/empresa/citas/{id}/seleccionar-bloque
     * El gestor elige el bloque de disponibilidad propuesto por el cliente
     * y reprograma la cita (actualiza Google Calendar si aplica).
     */
    @PreAuthorize("hasAnyAuthority('AGENDA_EDITAR', 'ADMIN_TOTAL')")
    @PatchMapping("/citas/{id}/seleccionar-bloque")
    public ResponseEntity<CitaResponse> seleccionarBloque(
            @PathVariable UUID id,
            @Valid @RequestBody SeleccionarBloqueRequest request) {

        return ResponseEntity.ok(agendaService.seleccionarBloqueDisponibilidad(id, request));
    }

    /**
     * GET /api/agenda/empresa/citas/activo/{activoId}
     * Lista todas las citas de una unidad inmobiliaria específica.
     */
    @PreAuthorize("hasAnyAuthority('AGENDA_VER', 'ADMIN_TOTAL')")
    @GetMapping("/citas/activo/{activoId}")
    public ResponseEntity<List<CitaResponse>> listarPorActivo(@PathVariable UUID activoId) {
        return ResponseEntity.ok(agendaService.listarCitasPorActivo(activoId));
    }

    /**
     * GET /api/agenda/empresa/citas/calendario
     * Retorna las citas del gestor autenticado en un rango de fechas.
     * Útil para renderizar el calendario mensual del backoffice.
     *
     * Params: inicio (ISO 8601), fin (ISO 8601)
     */
    @PreAuthorize("hasAnyAuthority('AGENDA_VER', 'ADMIN_TOTAL')")
    @GetMapping("/citas/calendario")
    public ResponseEntity<List<CitaResponse>> listarCalendarioGestor(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        String gestorUid = extraerUid(auth);
        return ResponseEntity.ok(agendaService.listarCitasGestor(gestorUid, inicio, fin));
    }

    /**
     * GET /api/agenda/empresa/citas
     * Lista todas las citas del sistema (solo ADMIN).
     */
    @PreAuthorize("hasAuthority('ADMIN_TOTAL')")
    @GetMapping("/citas")
    public ResponseEntity<List<CitaResponse>> listarTodas() {
        return ResponseEntity.ok(agendaService.listarTodasLasCitas());
    }

    /**
     * POST /api/agenda/empresa/sincronizar
     * Dispara manualmente el worker de reintentos de sincronización.
     * Útil en caso de incidentes con Google Calendar API.
     */
    @PreAuthorize("hasAuthority('ADMIN_TOTAL')")
    @PostMapping("/sincronizar")
    public ResponseEntity<Map<String, String>> sincronizarManual() {
        agendaService.reintentarSincronizacionesPendientes();
        return ResponseEntity.ok(Map.of("mensaje",
                "Proceso de sincronización ejecutado. Revisa los logs para el resultado."));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private String extraerUid(Authentication auth) {
        if (auth instanceof FirebaseAuthenticationToken token) {
            return (String) token.getPrincipal();
        }
        throw new IllegalStateException("Token de autenticación inválido");
    }
}
