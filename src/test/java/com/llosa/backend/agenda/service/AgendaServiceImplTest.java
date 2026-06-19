package com.llosa.backend.agenda.service;

import com.llosa.backend.agenda.dto.request.*;
import com.llosa.backend.agenda.dto.response.CitaResponse;
import com.llosa.backend.agenda.entity.Cita;
import com.llosa.backend.agenda.entity.DisponibilidadCita;
import com.llosa.backend.agenda.enums.EstadoCita;
import com.llosa.backend.agenda.enums.EstadoSincronizacion;
import com.llosa.backend.agenda.enums.TipoEvento;
import com.llosa.backend.agenda.repository.CitaRepository;
import com.llosa.backend.agenda.repository.DisponibilidadCitaRepository;
import com.llosa.backend.agenda.service.impl.AgendaServiceImpl;
import com.llosa.backend.agenda.service.impl.GoogleCalendarServiceImpl;
import com.llosa.backend.exception.AccesoDenegadoException;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendaServiceImplTest {

    @Mock CitaRepository citaRepository;
    @Mock DisponibilidadCitaRepository disponibilidadRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ActivoRepository activoRepository;
    @Mock GoogleCalendarServiceImpl googleCalendarService;

    @InjectMocks AgendaServiceImpl agendaService;

    private final String gestorFirebaseUid = "gestor-uid";
    private final String clienteFirebaseUid = "cliente-uid";
    private final UUID citaId = UUID.randomUUID();
    private final UUID activoId = UUID.randomUUID();

    private Usuario buildGestor() {
        var u = new Usuario();
        u.setId(1);
        u.setFirebaseUuid(gestorFirebaseUid);
        u.setNombre("Gestor");
        u.setApellidos("Test");
        u.setEmail("gestor@test.com");
        u.setTipoUsuario("ADMIN");
        return u;
    }

    private Usuario buildCliente() {
        var u = new Usuario();
        u.setId(2);
        u.setFirebaseUuid(clienteFirebaseUid);
        u.setNombre("Cliente");
        u.setApellidos("Test");
        u.setEmail("cliente@test.com");
        u.setTipoUsuario("CLIENTE");
        return u;
    }

    private Activo buildActivo() {
        return Activo.builder()
                .id(activoId)
                .nro("A-101")
                .tipo(TipoActivo.DEPARTAMENTO)
                .areaM2(new BigDecimal("80"))
                .build();
    }

    private Cita buildCita(EstadoCita estado, Usuario gestor, Usuario cliente) {
        return Cita.builder()
                .id(citaId)
                .gestor(gestor)
                .cliente(cliente)
                .activo(buildActivo())
                .tipoEvento(TipoEvento.ENTREGA_LLAVES)
                .titulo("Entrega de llaves")
                .fechaInicio(LocalDateTime.now().plusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(1).plusHours(1))
                .estadoCita(estado)
                .permiteReprogramacion(true)
                .clienteUsaGoogle(false)
                .estadoSincronizacion(EstadoSincronizacion.NO_APLICA)
                .disponibilidades(new ArrayList<>())
                .build();
    }

    // ─── crearCita ────────────────────────────────────────────────────

    @Test
    void crearCita_exitoso() {
        var gestor = buildGestor();
        var cliente = buildCliente();
        var activo = buildActivo();
        var req = new CrearCitaRequest(
                2, activoId, TipoEvento.ENTREGA_LLAVES, "Titulo", "Desc",
                "Oficina", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(1),
                true, false);

        when(usuarioRepository.findByFirebaseUuid(gestorFirebaseUid)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(cliente));
        when(activoRepository.findById(activoId)).thenReturn(Optional.of(activo));
        when(citaRepository.existeSolapamiento(anyInt(), any(), any())).thenReturn(false);
        when(citaRepository.save(any())).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            var idField = Cita.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(c, citaId);
            return c;
        });

        var result = agendaService.crearCita(gestorFirebaseUid, req);

        assertThat(result.id()).isEqualTo(citaId);
        assertThat(result.estadoCita()).isEqualTo(EstadoCita.PROGRAMADA);
    }

    @Test
    void crearCita_fechaFinNoPosterior_lanzaBusinessException() {
        var req = new CrearCitaRequest(2, activoId, TipoEvento.ENTREGA_LLAVES, null, null,
                null, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(0), true, false);

        assertThatThrownBy(() -> agendaService.crearCita(gestorFirebaseUid, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    void crearCita_clienteNoExiste_lanzaRecursoNoEncontrado() {
        var gestor = buildGestor();
        var req = new CrearCitaRequest(99, activoId, TipoEvento.ENTREGA_LLAVES, null, null,
                null, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1),
                true, false);

        when(usuarioRepository.findByFirebaseUuid(gestorFirebaseUid)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendaService.crearCita(gestorFirebaseUid, req))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearCita_activoNoExiste_lanzaRecursoNoEncontrado() {
        var gestor = buildGestor();
        var cliente = buildCliente();
        var req = new CrearCitaRequest(2, activoId, TipoEvento.ENTREGA_LLAVES, null, null,
                null, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1),
                true, false);

        when(usuarioRepository.findByFirebaseUuid(gestorFirebaseUid)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(cliente));
        when(activoRepository.findById(activoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendaService.crearCita(gestorFirebaseUid, req))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearCita_solapamiento_lanzaBusinessException() {
        var gestor = buildGestor();
        var cliente = buildCliente();
        var activo = buildActivo();
        var req = new CrearCitaRequest(2, activoId, TipoEvento.ENTREGA_LLAVES, null, null,
                null, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1),
                true, false);

        when(usuarioRepository.findByFirebaseUuid(gestorFirebaseUid)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(cliente));
        when(activoRepository.findById(activoId)).thenReturn(Optional.of(activo));
        when(citaRepository.existeSolapamiento(anyInt(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> agendaService.crearCita(gestorFirebaseUid, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene una cita en ese rango");
    }

    @Test
    void crearCita_conGoogleCalendar_sincroniza() {
        var gestor = buildGestor();
        var cliente = buildCliente();
        var activo = buildActivo();
        var req = new CrearCitaRequest(2, activoId, TipoEvento.ENTREGA_LLAVES, "Titulo", null,
                null, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1),
                true, true);

        when(usuarioRepository.findByFirebaseUuid(gestorFirebaseUid)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(cliente));
        when(activoRepository.findById(activoId)).thenReturn(Optional.of(activo));
        when(citaRepository.existeSolapamiento(anyInt(), any(), any())).thenReturn(false);
        when(googleCalendarService.crearEvento(any())).thenReturn(Optional.of("google-event-id"));
        when(citaRepository.save(any())).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            var idField = Cita.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(c, citaId);
            return c;
        });

        var result = agendaService.crearCita(gestorFirebaseUid, req);

        assertThat(result.estadoSincronizacion()).isEqualTo(EstadoSincronizacion.SINCRONIZADO);
        verify(googleCalendarService).crearEvento(any());
    }

    @Test
    void crearCita_conGoogleCalendar_fallo_sincronizacionPendiente() {
        var gestor = buildGestor();
        var cliente = buildCliente();
        var activo = buildActivo();
        var req = new CrearCitaRequest(2, activoId, TipoEvento.ENTREGA_LLAVES, "Titulo", null,
                null, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1),
                true, true);

        when(usuarioRepository.findByFirebaseUuid(gestorFirebaseUid)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(cliente));
        when(activoRepository.findById(activoId)).thenReturn(Optional.of(activo));
        when(citaRepository.existeSolapamiento(anyInt(), any(), any())).thenReturn(false);
        when(googleCalendarService.crearEvento(any())).thenReturn(Optional.empty());
        when(citaRepository.save(any())).thenAnswer(inv -> {
            Cita c = inv.getArgument(0);
            var idField = Cita.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(c, citaId);
            return c;
        });

        var result = agendaService.crearCita(gestorFirebaseUid, req);

        assertThat(result.estadoSincronizacion()).isEqualTo(EstadoSincronizacion.PENDIENTE);
    }

    // ─── actualizarCita ───────────────────────────────────────────────

    @Test
    void actualizarCita_exitoso() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());
        var req = new ActualizarCitaRequest("Nuevo titulo", null, null, null, null, null, null, null);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(citaRepository.save(any())).thenReturn(cita);

        var result = agendaService.actualizarCita(citaId, req);

        assertThat(result.titulo()).isEqualTo("Nuevo titulo");
    }

    @Test
    void actualizarCita_cambiaFecha_validaRango() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());
        var req = new ActualizarCitaRequest(null, null, null,
                LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(2), null, null, null);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> agendaService.actualizarCita(citaId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    void actualizarCita_conEstadoYMotivo() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());
        var req = new ActualizarCitaRequest(null, null, null, null, null,
                EstadoCita.CANCELADA, null, "Motivo test");

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(citaRepository.save(any())).thenReturn(cita);

        var result = agendaService.actualizarCita(citaId, req);

        assertThat(result.estadoCita()).isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    void actualizarCita_conGoogleActualizaEvento() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());
        cita.setGoogleEventId("google-id");
        cita.setEstadoSincronizacion(EstadoSincronizacion.SINCRONIZADO);
        var req = new ActualizarCitaRequest(null, null, null,
                LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1),
                null, null, null);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(googleCalendarService.actualizarEvento(cita)).thenReturn(true);
        when(citaRepository.save(any())).thenReturn(cita);

        agendaService.actualizarCita(citaId, req);

        verify(googleCalendarService).actualizarEvento(cita);
    }

    @Test
    void actualizarCita_falloGoogle_marcaPendiente() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());
        cita.setGoogleEventId("google-id");
        var req = new ActualizarCitaRequest(null, null, null,
                LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1),
                null, null, null);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(googleCalendarService.actualizarEvento(cita)).thenReturn(false);
        when(citaRepository.save(any())).thenReturn(cita);

        agendaService.actualizarCita(citaId, req);

        assertThat(cita.getEstadoSincronizacion()).isEqualTo(EstadoSincronizacion.PENDIENTE);
    }

    @Test
    void actualizarCita_noExiste_lanzaRecursoNoEncontrado() {
        when(citaRepository.findById(citaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendaService.actualizarCita(citaId, mock(ActualizarCitaRequest.class)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ─── cancelarCita ─────────────────────────────────────────────────

    @Test
    void cancelarCita_exitoso() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(citaRepository.save(any())).thenReturn(cita);

        var result = agendaService.cancelarCita(citaId, "Motivo");

        assertThat(result.estadoCita()).isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    void cancelarCita_yaCompletada_lanzaBusinessException() {
        var cita = buildCita(EstadoCita.COMPLETADA, buildGestor(), buildCliente());

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> agendaService.cancelarCita(citaId, "Motivo"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("completada");
    }

    @Test
    void cancelarCita_conGoogleEvent_eliminaEvento() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());
        cita.setGoogleEventId("google-id");

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(citaRepository.save(any())).thenReturn(cita);

        agendaService.cancelarCita(citaId, "Motivo");

        verify(googleCalendarService).eliminarEvento(cita);
        assertThat(cita.getGoogleEventId()).isNull();
    }

    // ─── seleccionarBloqueDisponibilidad ──────────────────────────────

    @Test
    void seleccionarBloqueDisponibilidad_exitoso() {
        var cita = buildCita(EstadoCita.REPROGRAMACION_PENDIENTE, buildGestor(), buildCliente());
        var bloque = DisponibilidadCita.builder()
                .id(1L).cita(cita)
                .bloqueInicio(LocalDateTime.now().plusDays(5))
                .bloqueFin(LocalDateTime.now().plusDays(5).plusHours(1))
                .build();

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(disponibilidadRepository.findById(1L)).thenReturn(Optional.of(bloque));
        when(citaRepository.save(any())).thenReturn(cita);

        var result = agendaService.seleccionarBloqueDisponibilidad(
                citaId, new SeleccionarBloqueRequest(1L));

        assertThat(result.estadoCita()).isEqualTo(EstadoCita.PROGRAMADA);
        verify(disponibilidadRepository).deseleccionarTodos(citaId);
        verify(disponibilidadRepository).save(bloque);
    }

    @Test
    void seleccionarBloque_noPermiteReprogramacion_lanzaBusinessException() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());
        cita.setPermiteReprogramacion(false);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> agendaService.seleccionarBloqueDisponibilidad(
                citaId, new SeleccionarBloqueRequest(1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no permite reprogramación");
    }

    @Test
    void seleccionarBloque_bloqueNoPertenece_lanzaAccesoDenegado() {
        var cita = buildCita(EstadoCita.REPROGRAMACION_PENDIENTE, buildGestor(), buildCliente());
        var otroCita = Cita.builder().id(UUID.randomUUID()).build();
        var bloque = DisponibilidadCita.builder().id(1L).cita(otroCita).build();

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(disponibilidadRepository.findById(1L)).thenReturn(Optional.of(bloque));

        assertThatThrownBy(() -> agendaService.seleccionarBloqueDisponibilidad(
                citaId, new SeleccionarBloqueRequest(1L)))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void seleccionarBloque_conGoogleEvent_actualiza() {
        var cita = buildCita(EstadoCita.REPROGRAMACION_PENDIENTE, buildGestor(), buildCliente());
        cita.setGoogleEventId("google-id");
        var bloque = DisponibilidadCita.builder()
                .id(1L).cita(cita)
                .bloqueInicio(LocalDateTime.now().plusDays(5))
                .bloqueFin(LocalDateTime.now().plusDays(5).plusHours(1))
                .build();

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(disponibilidadRepository.findById(1L)).thenReturn(Optional.of(bloque));
        when(googleCalendarService.actualizarEvento(cita)).thenReturn(true);
        when(citaRepository.save(any())).thenReturn(cita);

        agendaService.seleccionarBloqueDisponibilidad(citaId, new SeleccionarBloqueRequest(1L));

        verify(googleCalendarService).actualizarEvento(cita);
    }

    // ─── listar ───────────────────────────────────────────────────────

    @Test
    void listarCitasPorActivo_retornaLista() {
        when(citaRepository.findByActivoId(activoId)).thenReturn(List.of());
        assertThat(agendaService.listarCitasPorActivo(activoId)).isEmpty();
    }

    @Test
    void listarCitasGestor_retornaLista() {
        var gestor = buildGestor();
        when(usuarioRepository.findByFirebaseUuid(gestorFirebaseUid)).thenReturn(Optional.of(gestor));
        when(citaRepository.findByGestorAndRango(anyInt(), any(), any())).thenReturn(List.of());

        var result = agendaService.listarCitasGestor(gestorFirebaseUid, LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        assertThat(result).isEmpty();
    }

    @Test
    void listarTodasLasCitas_retornaLista() {
        when(citaRepository.findAll()).thenReturn(List.of());
        assertThat(agendaService.listarTodasLasCitas()).isEmpty();
    }

    @Test
    void listarCitasCliente_retornaLista() {
        var cliente = buildCliente();
        when(usuarioRepository.findByFirebaseUuid(clienteFirebaseUid)).thenReturn(Optional.of(cliente));
        when(citaRepository.findByClienteId(2)).thenReturn(List.of());

        assertThat(agendaService.listarCitasCliente(clienteFirebaseUid)).isEmpty();
    }

    @Test
    void listarProximasCitasCliente_retornaLista() {
        var cliente = buildCliente();
        when(usuarioRepository.findByFirebaseUuid(clienteFirebaseUid)).thenReturn(Optional.of(cliente));
        when(citaRepository.findProximasByClienteId(anyInt(), any())).thenReturn(List.of());

        assertThat(agendaService.listarProximasCitasCliente(clienteFirebaseUid)).isEmpty();
    }

    @Test
    void listarCitasGestor_usuarioNoExiste_lanzaRecursoNoEncontrado() {
        when(usuarioRepository.findByFirebaseUuid(gestorFirebaseUid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendaService.listarCitasGestor(gestorFirebaseUid, LocalDateTime.now(), LocalDateTime.now()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ─── responderCita ────────────────────────────────────────────────

    @Test
    void responderCita_confirmada() {
        var cliente = buildCliente();
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), cliente);
        var req = new RespuestaClienteRequest(true, "OK");

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByFirebaseUuid(clienteFirebaseUid)).thenReturn(Optional.of(cliente));
        when(citaRepository.save(any())).thenReturn(cita);

        var result = agendaService.responderCita(citaId, clienteFirebaseUid, req);

        assertThat(result.estadoCita()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(result.confirmacionCliente()).isTrue();
    }

    @Test
    void responderCita_declinada() {
        var cliente = buildCliente();
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), cliente);
        var req = new RespuestaClienteRequest(false, "No puedo");

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByFirebaseUuid(clienteFirebaseUid)).thenReturn(Optional.of(cliente));
        when(citaRepository.save(any())).thenReturn(cita);

        var result = agendaService.responderCita(citaId, clienteFirebaseUid, req);

        assertThat(result.estadoCita()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(result.confirmacionCliente()).isFalse();
    }

    @Test
    void responderCita_noPertenece_lanzaAccesoDenegado() {
        var otroCliente = new Usuario();
        otroCliente.setId(3);
        otroCliente.setFirebaseUuid("otro-uid");
        var clienteAutenticado = buildCliente();
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), otroCliente);
        var req = new RespuestaClienteRequest(true, null);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByFirebaseUuid(clienteFirebaseUid)).thenReturn(Optional.of(clienteAutenticado));

        assertThatThrownBy(() -> agendaService.responderCita(citaId, clienteFirebaseUid, req))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void responderCita_cancelada_lanzaBusinessException() {
        var cliente = buildCliente();
        var cita = buildCita(EstadoCita.CANCELADA, buildGestor(), cliente);
        var req = new RespuestaClienteRequest(true, null);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByFirebaseUuid(clienteFirebaseUid)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> agendaService.responderCita(citaId, clienteFirebaseUid, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede responder");
    }

    @Test
    void responderCita_conGoogleCalendar_actualizaRsvp() {
        var cliente = buildCliente();
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), cliente);
        cita.setGoogleEventId("google-id");
        cita.setClienteUsaGoogle(true);
        var req = new RespuestaClienteRequest(true, null);

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByFirebaseUuid(clienteFirebaseUid)).thenReturn(Optional.of(cliente));
        when(citaRepository.save(any())).thenReturn(cita);

        agendaService.responderCita(citaId, clienteFirebaseUid, req);

        verify(googleCalendarService).actualizarRsvpInvitado(cita.getGestor(), "google-id", cliente.getEmail(), true);
    }

    // ─── proponerDisponibilidad ───────────────────────────────────────

    @Test
    void proponerDisponibilidad_exitoso() {
        var cliente = buildCliente();
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), cliente);
        var req = new DisponibilidadRequest(List.of(
                new DisponibilidadRequest.BloqueHorario(
                        LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(10).plusHours(1))));

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByFirebaseUuid(clienteFirebaseUid)).thenReturn(Optional.of(cliente));
        when(disponibilidadRepository.saveAll(anyList())).thenReturn(List.of());

        var result = agendaService.proponerDisponibilidad(citaId, clienteFirebaseUid, req);

        assertThat(result).isEmpty();
        assertThat(cita.getEstadoCita()).isEqualTo(EstadoCita.REPROGRAMACION_PENDIENTE);
    }

    @Test
    void proponerDisponibilidad_noPermiteReprogramacion_lanzaBusinessException() {
        var cliente = buildCliente();
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), cliente);
        cita.setPermiteReprogramacion(false);
        var req = new DisponibilidadRequest(List.of());

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByFirebaseUuid(clienteFirebaseUid)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> agendaService.proponerDisponibilidad(citaId, clienteFirebaseUid, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no permite reprogramación");
    }

    @Test
    void proponerDisponibilidad_noPertenece_lanzaAccesoDenegado() {
        var otroCliente = new Usuario();
        otroCliente.setId(3);
        otroCliente.setFirebaseUuid("otro-uid");
        var cliente = buildCliente();
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), otroCliente);
        var req = new DisponibilidadRequest(List.of());

        when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
        when(usuarioRepository.findByFirebaseUuid(clienteFirebaseUid)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> agendaService.proponerDisponibilidad(citaId, clienteFirebaseUid, req))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    // ─── reintentarSincronizacionesPendientes ─────────────────────────

    @Test
    void reintentarSincronizacionesPendientes_sinPendientes_noHaceNada() {
        when(citaRepository.findByEstadoSincronizacionAndClienteUsaGoogle(
                EstadoSincronizacion.PENDIENTE, true)).thenReturn(List.of());

        agendaService.reintentarSincronizacionesPendientes();

        verify(googleCalendarService, never()).crearEvento(any());
        verify(googleCalendarService, never()).actualizarEvento(any());
    }

    @Test
    void reintentarSincronizacionesPendientes_sinGoogleEventId_intentaCrear() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());
        cita.setClienteUsaGoogle(true);
        cita.setEstadoSincronizacion(EstadoSincronizacion.PENDIENTE);
        cita.setGoogleEventId(null);

        when(citaRepository.findByEstadoSincronizacionAndClienteUsaGoogle(
                EstadoSincronizacion.PENDIENTE, true)).thenReturn(List.of(cita));
        when(citaRepository.save(any())).thenReturn(cita);

        agendaService.reintentarSincronizacionesPendientes();

        verify(googleCalendarService).crearEvento(cita);
    }

    @Test
    void reintentarSincronizacionesPendientes_conGoogleEventId_actualiza() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());
        cita.setClienteUsaGoogle(true);
        cita.setEstadoSincronizacion(EstadoSincronizacion.PENDIENTE);
        cita.setGoogleEventId("google-id");

        when(citaRepository.findByEstadoSincronizacionAndClienteUsaGoogle(
                EstadoSincronizacion.PENDIENTE, true)).thenReturn(List.of(cita));
        when(googleCalendarService.actualizarEvento(cita)).thenReturn(true);
        when(citaRepository.save(any())).thenReturn(cita);

        agendaService.reintentarSincronizacionesPendientes();

        assertThat(cita.getEstadoSincronizacion()).isEqualTo(EstadoSincronizacion.SINCRONIZADO);
    }

    @Test
    void reintentarSincronizacionesPendientes_fallo_capturaExcepcion() {
        var cita = buildCita(EstadoCita.PROGRAMADA, buildGestor(), buildCliente());
        cita.setClienteUsaGoogle(true);
        cita.setEstadoSincronizacion(EstadoSincronizacion.PENDIENTE);
        cita.setGoogleEventId(null);

        when(citaRepository.findByEstadoSincronizacionAndClienteUsaGoogle(
                EstadoSincronizacion.PENDIENTE, true)).thenReturn(List.of(cita));
        when(googleCalendarService.crearEvento(any())).thenThrow(new RuntimeException("API Error"));

        assertThatCode(() -> agendaService.reintentarSincronizacionesPendientes())
                .doesNotThrowAnyException();
    }
}
