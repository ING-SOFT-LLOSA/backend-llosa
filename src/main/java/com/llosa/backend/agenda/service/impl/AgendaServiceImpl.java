package com.llosa.backend.agenda.service.impl;

import com.llosa.backend.agenda.dto.request.*;
import com.llosa.backend.agenda.dto.response.CitaResponse;
import com.llosa.backend.agenda.dto.response.DisponibilidadResponse;
import com.llosa.backend.agenda.entity.Cita;
import com.llosa.backend.agenda.entity.DisponibilidadCita;
import com.llosa.backend.agenda.enums.EstadoCita;
import com.llosa.backend.agenda.enums.EstadoSincronizacion;
import com.llosa.backend.agenda.enums.TipoEvento;
import com.llosa.backend.agenda.repository.CitaRepository;
import com.llosa.backend.agenda.repository.DisponibilidadCitaRepository;
import com.llosa.backend.agenda.service.AgendaService;
import com.llosa.backend.agenda.service.GoogleCalendarService;
import com.llosa.backend.agenda.service.impl.GoogleCalendarServiceImpl;
import com.llosa.backend.exception.AccesoDenegadoException;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgendaServiceImpl implements AgendaService {

    private final CitaRepository citaRepository;
    private final DisponibilidadCitaRepository disponibilidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final ActivoRepository activoRepository;
    private final GoogleCalendarService googleCalendarService;

    // ── Portal Empresa ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CitaResponse crearCita(String gestorFirebaseUid, CrearCitaRequest req) {

        // Validar rango cronológico
        if (!req.fechaFin().isAfter(req.fechaInicio())) {
            throw new BusinessException("La fecha de fin debe ser posterior a la de inicio");
        }

        Usuario gestor = resolverPorFirebaseUid(gestorFirebaseUid);
        Usuario cliente = usuarioRepository.findById(req.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cliente no encontrado: " + req.clienteId()));
        Activo activo = activoRepository.findById(req.activoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Unidad no encontrada: " + req.activoId()));

        // Verificar solapamiento de agenda del gestor (salta para recordatorios de pago)
        if (req.tipoEvento() != TipoEvento.RECORDATORIO_PAGO
                && citaRepository.existeSolapamiento(gestor.getId(), req.fechaInicio(), req.fechaFin())) {
            throw new BusinessException(
                    "El gestor ya tiene una cita en ese rango horario. Elige otro horario.");
        }

        boolean sincronizar = req.clienteUsaGoogle()
                || req.tipoEvento() == TipoEvento.RECORDATORIO_PAGO;

        Cita cita = Cita.builder()
                .gestor(gestor)
                .cliente(cliente)
                .activo(activo)
                .tipoEvento(req.tipoEvento())
                .titulo(req.titulo())
                .descripcion(req.descripcion())
                .ubicacion(req.ubicacion())
                .fechaInicio(req.fechaInicio())
                .fechaFin(req.fechaFin())
                .permiteReprogramacion(req.permiteReprogramacion())
                .clienteUsaGoogle(req.clienteUsaGoogle())
                .estadoCita(EstadoCita.PROGRAMADA)
                .estadoSincronizacion(sincronizar
                        ? EstadoSincronizacion.PENDIENTE
                        : EstadoSincronizacion.NO_APLICA)
                .build();

        cita = citaRepository.save(cita);

        if (sincronizar) {
            sincronizarCreacion(cita);
        } else {
            log.info("[Agenda] Cita {} guardada solo en BD (cliente sin Google)", cita.getId());
        }

        return CitaResponse.fromEntity(cita);
    }

    @Override
    @Transactional
    public CitaResponse actualizarCita(UUID citaId, ActualizarCitaRequest req) {
        Cita cita = obtenerCita(citaId);

        // Aplicar campos opcionales
        if (req.titulo() != null)       cita.setTitulo(req.titulo());
        if (req.descripcion() != null)  cita.setDescripcion(req.descripcion());
        if (req.ubicacion() != null)    cita.setUbicacion(req.ubicacion());
        if (req.permiteReprogramacion() != null)
            cita.setPermiteReprogramacion(req.permiteReprogramacion());

        boolean fechaCambiada = false;
        if (req.fechaInicio() != null && req.fechaFin() != null) {
            if (!req.fechaFin().isAfter(req.fechaInicio())) {
                throw new BusinessException("La fecha de fin debe ser posterior a la de inicio");
            }
            cita.setFechaInicio(req.fechaInicio());
            cita.setFechaFin(req.fechaFin());
            fechaCambiada = true;
        }

        if (req.estadoCita() != null) {
            cita.setEstadoCita(req.estadoCita());
            if (req.motivoCancelacion() != null) {
                cita.setMotivoCancelacion(req.motivoCancelacion());
            }
        }

        cita = citaRepository.save(cita);

        // Re-sincronizar con Google Calendar si tiene evento y algo cambió
        if (cita.getGoogleEventId() != null && fechaCambiada) {
            boolean ok = googleCalendarService.actualizarEvento(cita);
            if (!ok) {
                cita.setEstadoSincronizacion(EstadoSincronizacion.PENDIENTE);
                citaRepository.save(cita);
                log.warn("[Agenda] Fallo al actualizar evento Google para cita {}, encolado para reintento",
                        cita.getId());
            }
        }

        return CitaResponse.fromEntity(cita);
    }

    @Override
    @Transactional
    public CitaResponse cancelarCita(UUID citaId, String motivo) {
        Cita cita = obtenerCita(citaId);

        if (cita.getEstadoCita() == EstadoCita.COMPLETADA) {
            throw new BusinessException("No se puede cancelar una cita ya completada");
        }

        cita.setEstadoCita(EstadoCita.CANCELADA);
        cita.setMotivoCancelacion(motivo);

        // Eliminar de Google Calendar si existía evento
        if (cita.getGoogleEventId() != null) {
            // NOTA: usamos el overload que recibe la Cita completa, porque
            // necesitamos el gestor (dueño del token OAuth) para autenticar
            // la llamada a Google. El método de la interfaz que solo recibe
            // el String quedó como stub por compatibilidad.
            ((GoogleCalendarServiceImpl) googleCalendarService).eliminarEvento(cita);
            cita.setGoogleEventId(null);
            cita.setEstadoSincronizacion(EstadoSincronizacion.NO_APLICA);
        }

        return CitaResponse.fromEntity(citaRepository.save(cita));
    }

    @Override
    @Transactional
    public CitaResponse seleccionarBloqueDisponibilidad(UUID citaId, SeleccionarBloqueRequest req) {
        Cita cita = obtenerCita(citaId);

        if (!cita.getPermiteReprogramacion()) {
            throw new BusinessException("Esta cita no permite reprogramación");
        }

        DisponibilidadCita bloque = disponibilidadRepository.findById(req.bloqueId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Bloque de disponibilidad no encontrado: " + req.bloqueId()));

        if (!bloque.getCita().getId().equals(citaId)) {
            throw new AccesoDenegadoException("El bloque no pertenece a esta cita");
        }

        // Desmarcar todos los bloques anteriores
        disponibilidadRepository.deseleccionarTodos(citaId);

        // Seleccionar el nuevo bloque
        bloque.setSeleccionado(true);
        disponibilidadRepository.save(bloque);

        // Actualizar la cita con el nuevo horario
        cita.setFechaInicio(bloque.getBloqueInicio());
        cita.setFechaFin(bloque.getBloqueFin());
        cita.setEstadoCita(EstadoCita.PROGRAMADA);
        cita = citaRepository.save(cita);

        // Actualizar en Google Calendar si aplica
        if (cita.getGoogleEventId() != null) {
            boolean ok = googleCalendarService.actualizarEvento(cita);
            if (!ok) {
                cita.setEstadoSincronizacion(EstadoSincronizacion.PENDIENTE);
                citaRepository.save(cita);
            }
        }

        log.info("[Agenda] Gestor seleccionó bloque {} para cita {}. Nueva fecha: {}",
                bloque.getId(), citaId, bloque.getBloqueInicio());

        return CitaResponse.fromEntity(cita);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listarCitasPorActivo(UUID activoId) {
        return citaRepository.findByActivoId(activoId).stream()
                .map(CitaResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listarCitasGestor(String gestorFirebaseUid,
                                                LocalDateTime inicio,
                                                LocalDateTime fin) {
        Usuario gestor = resolverPorFirebaseUid(gestorFirebaseUid);
        return citaRepository.findByGestorAndRango(gestor.getId(), inicio, fin).stream()
                .map(CitaResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listarTodasLasCitas() {
        return citaRepository.findAll().stream()
                .map(CitaResponse::fromEntity)
                .toList();
    }

    // ── Portal Cliente ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listarCitasCliente(String clienteFirebaseUid) {
        Usuario cliente = resolverPorFirebaseUid(clienteFirebaseUid);
        return citaRepository.findByClienteId(cliente.getId()).stream()
                .map(CitaResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> listarProximasCitasCliente(String clienteFirebaseUid) {
        Usuario cliente = resolverPorFirebaseUid(clienteFirebaseUid);
        return citaRepository.findProximasByClienteId(cliente.getId(), LocalDateTime.now())
                .stream()
                .map(CitaResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public CitaResponse responderCita(UUID citaId,
                                      String clienteFirebaseUid,
                                      RespuestaClienteRequest req) {
        Cita cita = obtenerCita(citaId);
        Usuario cliente = resolverPorFirebaseUid(clienteFirebaseUid);

        // Verificar que la cita le pertenece
        if (!cita.getCliente().getId().equals(cliente.getId())) {
            throw new AccesoDenegadoException("No tienes permiso para responder esta cita");
        }

        if (cita.getEstadoCita() == EstadoCita.CANCELADA
                || cita.getEstadoCita() == EstadoCita.COMPLETADA) {
            throw new BusinessException("No se puede responder a una cita " + cita.getEstadoCita());
        }

        cita.setConfirmacionCliente(req.confirmado());
        cita.setEstadoCita(req.confirmado() ? EstadoCita.CONFIRMADA : EstadoCita.PROGRAMADA);

        // Actualizar RSVP en Google Calendar si aplica
        if (cita.getGoogleEventId() != null && Boolean.TRUE.equals(cita.getClienteUsaGoogle())) {
            // NOTA: usamos el overload que recibe el gestor (Usuario), porque
            // necesitamos su token OAuth para autenticar la llamada a Google.
            // El método de la interfaz que solo recibe el String quedó como
            // stub por compatibilidad.
            ((GoogleCalendarServiceImpl) googleCalendarService).actualizarRsvpInvitado(
                    cita.getGestor(),
                    cita.getGoogleEventId(),
                    cliente.getEmail(),
                    req.confirmado()
            );
        }

        log.info("[Agenda] Cliente {} respondió cita {}: confirmado={}",
                cliente.getId(), citaId, req.confirmado());

        return CitaResponse.fromEntity(citaRepository.save(cita));
    }

    @Override
    @Transactional
    public List<DisponibilidadResponse> proponerDisponibilidad(UUID citaId,
                                                               String clienteFirebaseUid,
                                                               DisponibilidadRequest req) {
        Cita cita = obtenerCita(citaId);
        Usuario cliente = resolverPorFirebaseUid(clienteFirebaseUid);

        // Verificar pertenencia
        if (!cita.getCliente().getId().equals(cliente.getId())) {
            throw new AccesoDenegadoException("No tienes permiso para proponer disponibilidad en esta cita");
        }

        // Verificar que la reprogramación esté habilitada
        if (!cita.getPermiteReprogramacion()) {
            throw new BusinessException(
                    "Esta cita no permite reprogramación. Contacta con tu asesor.");
        }

        // Marcar la cita como en espera de reprogramación
        cita.setEstadoCita(EstadoCita.REPROGRAMACION_PENDIENTE);
        citaRepository.save(cita);

        // Persistir los bloques propuestos
        List<DisponibilidadCita> bloques = req.bloques().stream()
                .map(b -> DisponibilidadCita.builder()
                        .cita(cita)
                        .bloqueInicio(b.inicio())
                        .bloqueFin(b.fin())
                        .seleccionado(false)
                        .build())
                .toList();

        List<DisponibilidadCita> guardados = disponibilidadRepository.saveAll(bloques);

        log.info("[Agenda] Cliente {} propuso {} bloques para cita {}",
                cliente.getId(), bloques.size(), citaId);

        return guardados.stream()
                .map(DisponibilidadResponse::fromEntity)
                .toList();
    }

    // ── Worker de reintentos ──────────────────────────────────────────────────

    /**
     * Se ejecuta cada 15 minutos para reintentar las sincronizaciones fallidas.
     * Esto garantiza que ninguna cita quede desincronizada por un fallo temporal
     * de la API de Google Calendar.
     */
    @Scheduled(fixedDelay = 900_000)  // cada 15 minutos
    @Transactional
    @Override
    public void reintentarSincronizacionesPendientes() {
        List<Cita> pendientes = new java.util.ArrayList<>();
        pendientes.addAll(citaRepository
                .findByEstadoSincronizacionAndClienteUsaGoogle(
                        EstadoSincronizacion.PENDIENTE, true));
        pendientes.addAll(citaRepository
                .findByEstadoSincronizacionAndTipoEvento(
                        EstadoSincronizacion.PENDIENTE, TipoEvento.RECORDATORIO_PAGO));

        if (pendientes.isEmpty()) return;

        log.info("[Agenda-Worker] Reintentando sincronización de {} citas pendientes",
                pendientes.size());

        for (Cita cita : pendientes) {
            try {
                if (cita.getGoogleEventId() == null) {
                    // Nunca se creó el evento: intentar creación
                    sincronizarCreacion(cita);
                } else {
                    // Ya existe el evento pero hubo fallo al actualizar
                    boolean ok = googleCalendarService.actualizarEvento(cita);
                    if (ok) {
                        cita.setEstadoSincronizacion(EstadoSincronizacion.SINCRONIZADO);
                        citaRepository.save(cita);
                        log.info("[Agenda-Worker] Cita {} re-sincronizada", cita.getId());
                    }
                }
            } catch (Exception e) {
                log.error("[Agenda-Worker] Fallo reintento cita {}: {}", cita.getId(), e.getMessage());
            }
        }
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private void sincronizarCreacion(Cita cita) {
        Optional<String> eventId = googleCalendarService.crearEvento(cita);
        if (eventId.isPresent()) {
            cita.setGoogleEventId(eventId.get());
            cita.setEstadoSincronizacion(EstadoSincronizacion.SINCRONIZADO);
            log.info("[Agenda] Cita {} sincronizada con Google Calendar. EventID={}",
                    cita.getId(), eventId.get());
        } else {
            cita.setEstadoSincronizacion(EstadoSincronizacion.PENDIENTE);
            log.warn("[Agenda] Fallo al sincronizar cita {} con Google Calendar. " +
                    "Encolada para reintento.", cita.getId());
        }
        citaRepository.save(cita);
    }

    private Cita obtenerCita(UUID citaId) {
        return citaRepository.findById(citaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + citaId));
    }

    private Usuario resolverPorFirebaseUid(String firebaseUid) {
        return usuarioRepository.findByFirebaseUuid(firebaseUid)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado para UID: " + firebaseUid));
    }
}