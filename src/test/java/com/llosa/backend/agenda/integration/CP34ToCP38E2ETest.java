package com.llosa.backend.agenda.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.llosa.backend.annotation.CP;
import com.llosa.backend.agenda.dto.request.CrearCitaRequest;
import com.llosa.backend.agenda.dto.request.DisponibilidadRequest;
import com.llosa.backend.agenda.dto.request.RespuestaClienteRequest;
import com.llosa.backend.agenda.entity.Cita;
import com.llosa.backend.agenda.enums.EstadoCita;
import com.llosa.backend.agenda.enums.EstadoSincronizacion;
import com.llosa.backend.agenda.enums.TipoEvento;
import com.llosa.backend.agenda.repository.CitaRepository;
import com.llosa.backend.agenda.repository.DisponibilidadCitaRepository;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.repository.PisoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.TorreRepository;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPRINT 6 — Módulo de Agenda y Comunicaciones (Citas + Google Calendar).
 * Casos de prueba CP34–CP38 (CU010 / CU013).
 *
 * Tests E2E FIELES al Plan de Pruebas: afirman lo que el sistema DEBE cumplir.
 * Si el backend no cumple un CP, el test falla (rojo) => defecto para Mantis.
 *
 * Sin Docker (H2, perfil test del equipo). Google Calendar NO se invoca de verdad:
 * los casos de cliente-sin-Google y de fallo de sync se cubren a nivel backend
 * (resguardo local + estado de sincronización), que es justamente lo testeable aquí.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
@Import({SecurityTestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CP34ToCP38E2ETest {

    private static final String GESTOR_UID = "gestor-agenda-uid";
    private static final String CLIENTE_UID = "cliente-agenda-uid";

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ProyectoRepository proyectoRepository;
    @Autowired TorreRepository torreRepository;
    @Autowired PisoRepository pisoRepository;
    @Autowired ActivoRepository activoRepository;
    @Autowired CitaRepository citaRepository;
    @Autowired DisponibilidadCitaRepository disponibilidadRepository;

    private Integer clienteId;
    private UUID activoId;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        disponibilidadRepository.deleteAll();
        citaRepository.deleteAll();
        activoRepository.deleteAll();
        pisoRepository.deleteAll();
        torreRepository.deleteAll();
        proyectoRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario gestor = new Usuario();
        gestor.setNombre("Gestor"); gestor.setApellidos("Postventa");
        gestor.setEmail("gestor@utec.edu.pe"); gestor.setTipoUsuario("EMPLEADO");
        gestor.setFirebaseUuid(GESTOR_UID); gestor.setActivo(true);
        usuarioRepository.save(gestor);

        Usuario cliente = new Usuario();
        cliente.setNombre("Cliente"); cliente.setApellidos("Comprador");
        cliente.setEmail("cliente@correo.com"); cliente.setTipoUsuario("CLIENTE");
        cliente.setFirebaseUuid(CLIENTE_UID); cliente.setActivo(true);
        clienteId = usuarioRepository.save(cliente).getId();

        Proyecto proyecto = proyectoRepository.save(Proyecto.builder().nombre("Edificio Agenda").build());
        Torre torre = torreRepository.save(Torre.builder().nombre("Torre A").proyecto(proyecto).build());
        Piso piso = pisoRepository.save(Piso.builder().nroPiso(1).torre(torre).build());
        activoId = activoRepository.save(Activo.builder()
                .nro("A-101").tipo(TipoActivo.DEPARTAMENTO)
                .estadoComercial(EstadoComercialActivo.VENDIDO).piso(piso).build())
                .getId();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP34: El gestor agenda una cita de visita para un cliente.
    //   Esperado (PDF): el sistema registra la cita y la deja PROGRAMADA.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP34",
        scenario = "Gestor agenda una cita y queda PROGRAMADA",
        input = "POST /api/agenda/empresa/citas con fechas futuras validas",
        expected = "201 + estadoCita=PROGRAMADA + cita persistida",
        type = CP.TestType.E2E)
    void cp34_gestorAgendaCita_quedaProgramada() throws Exception {
        var req = crearCitaRequest(LocalDateTime.now().plusDays(3), false);

        String resp = mockMvc.perform(post("/api/agenda/empresa/citas")
                        .with(securityContext(authGestor()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoCita").value("PROGRAMADA"))
                .andReturn().getResponse().getContentAsString();

        UUID citaId = UUID.fromString(objectMapper.readTree(resp).get("id").asText());
        assertThat(citaRepository.findById(citaId)).isPresent();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP35: Resguardo local cuando el cliente NO usa Google Calendar.
    //   Esperado (PDF): la cita se persiste íntegra en el backend aunque no haya
    //   sincronización con Google; el estado de sincronización debe ser NO_APLICA.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP35",
        scenario = "Cita de cliente sin Google se resguarda solo en backend",
        input = "POST /api/agenda/empresa/citas con clienteUsaGoogle=false",
        expected = "201 + estadoSincronizacion=NO_APLICA + cita en BD",
        type = CP.TestType.E2E)
    void cp35_clienteSinGoogle_resguardoLocal() throws Exception {
        var req = crearCitaRequest(LocalDateTime.now().plusDays(4), false);

        mockMvc.perform(post("/api/agenda/empresa/citas")
                        .with(securityContext(authGestor()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoSincronizacion").value("NO_APLICA"))
                .andExpect(jsonPath("$.googleEventId").doesNotExist());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP36: Rechazo de cita agendada en una fecha pasada.
    //   Esperado (PDF): el sistema valida la temporalidad y rechaza el agendado
    //   de una cita cuya fecha de inicio ya pasó.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP36",
        scenario = "Rechazar cita con fecha de inicio en el pasado",
        input = "POST /api/agenda/empresa/citas con fechaInicio = ayer",
        expected = "4xx: la cita debe ser en una fecha futura",
        type = CP.TestType.E2E)
    void cp36_citaEnFechaPasada_esRechazada() throws Exception {
        LocalDateTime pasado = LocalDateTime.now().minusDays(1);
        var req = new CrearCitaRequest(
                clienteId, activoId, TipoEvento.INSPECCION_OBRA,
                "Visita atrasada", "desc", "Oficina",
                pasado, pasado.plusHours(1), false, false);

        mockMvc.perform(post("/api/agenda/empresa/citas")
                        .with(securityContext(authGestor()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP37: El cliente confirma su asistencia a la cita.
    //   Esperado (PDF): la cita pasa a estado CONFIRMADA y se registra la
    //   confirmación del cliente.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP37",
        scenario = "Cliente confirma la cita y queda CONFIRMADA",
        input = "PATCH /api/agenda/cliente/citas/{id}/respuesta confirmado=true",
        expected = "200 + estadoCita=CONFIRMADA + confirmacionCliente=true",
        type = CP.TestType.E2E)
    void cp37_clienteConfirmaCita_quedaConfirmada() throws Exception {
        UUID citaId = persistirCitaProgramada(false);

        var req = new RespuestaClienteRequest(true, "Ahí estaré");

        mockMvc.perform(patch("/api/agenda/cliente/citas/{id}/respuesta", citaId)
                        .with(securityContext(authCliente()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCita").value("CONFIRMADA"))
                .andExpect(jsonPath("$.confirmacionCliente").value(true));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP38: El cliente propone bloques de disponibilidad para reprogramar.
    //   Esperado (PDF): si la cita permite reprogramación, el sistema registra los
    //   bloques propuestos y deja la cita en REPROGRAMACION_PENDIENTE.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP38",
        scenario = "Cliente propone disponibilidad y la cita queda en reprogramacion",
        input = "POST /api/agenda/cliente/citas/{id}/disponibilidad con 2 bloques",
        expected = "200 + 2 bloques persistidos + estadoCita=REPROGRAMACION_PENDIENTE",
        type = CP.TestType.E2E)
    void cp38_clienteProponeDisponibilidad_reprogramacionPendiente() throws Exception {
        UUID citaId = persistirCitaProgramada(true); // permiteReprogramacion = true

        LocalDateTime base = LocalDateTime.now().plusDays(6);
        var req = new DisponibilidadRequest(List.of(
                new DisponibilidadRequest.BloqueHorario(base, base.plusHours(1)),
                new DisponibilidadRequest.BloqueHorario(base.plusDays(1), base.plusDays(1).plusHours(1))));

        mockMvc.perform(post("/api/agenda/cliente/citas/{id}/disponibilidad", citaId)
                        .with(securityContext(authCliente()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        Cita cita = citaRepository.findById(citaId).orElseThrow();
        assertThat(cita.getEstadoCita()).isEqualTo(EstadoCita.REPROGRAMACION_PENDIENTE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CrearCitaRequest crearCitaRequest(LocalDateTime inicio, boolean permiteReprogramacion) {
        return new CrearCitaRequest(
                clienteId, activoId, TipoEvento.INSPECCION_OBRA,
                "Visita a la unidad", "Recorrido por el departamento", "Showroom",
                inicio, inicio.plusHours(1), permiteReprogramacion, false);
    }

    /** Crea la cita directamente vía endpoint y devuelve su id. */
    private UUID persistirCitaProgramada(boolean permiteReprogramacion) throws Exception {
        var req = crearCitaRequest(LocalDateTime.now().plusDays(5), permiteReprogramacion);
        String resp = mockMvc.perform(post("/api/agenda/empresa/citas")
                        .with(securityContext(authGestor()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(resp).get("id").asText());
    }

    private SecurityContext authGestor() {
        return contextWith(GESTOR_UID, "gestor@utec.edu.pe",
                new SimpleGrantedAuthority("AGENDA_CREAR"),
                new SimpleGrantedAuthority("AGENDA_EDITAR"),
                new SimpleGrantedAuthority("AGENDA_VER"));
    }

    private SecurityContext authCliente() {
        return contextWith(CLIENTE_UID, "cliente@correo.com",
                new SimpleGrantedAuthority("AGENDA_VER"));
    }

    private SecurityContext contextWith(String uid, String email, SimpleGrantedAuthority... auths) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new FirebaseAuthenticationToken(uid, email, List.of(auths)));
        return context;
    }
}
