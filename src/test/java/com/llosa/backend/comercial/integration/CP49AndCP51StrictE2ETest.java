package com.llosa.backend.comercial.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.llosa.backend.annotation.CP;
import com.llosa.backend.comercial.dto.EtapaExpedienteEstadoRequest;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.enums.EstadoEtapaExpediente;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.repository.PisoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.TorreRepository;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPRINT 9 — Estructura Física (CP49) y Transición de Etapas Comerciales (CP51).
 * Tests E2E ESTRICTOS y FIELES al Plan de Pruebas v3 (rev. 19/06/26): afirman EXACTAMENTE
 * lo que el documento dice que el sistema DEBE cumplir, no lo que el código hace cómodo.
 *
 * Los tests @Disabled documentan DEFECTOS reales (rojo) listos para Mantis.
 * Corre sobre H2 (perfil test, SIN Docker), @Tag("integration").
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
@Import({SecurityTestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CP49AndCP51StrictE2ETest {

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired ProyectoRepository proyectoRepository;
    @Autowired TorreRepository torreRepository;
    @Autowired PisoRepository pisoRepository;
    @Autowired ActivoRepository activoRepository;
    @Autowired UsuarioActivoRepository usuarioActivoRepository;
    @Autowired EtapaExpedienteRepository etapaExpedienteRepository;

    private Piso piso;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        etapaExpedienteRepository.deleteAll();
        usuarioActivoRepository.deleteAll();
        activoRepository.deleteAll();
        pisoRepository.deleteAll();
        torreRepository.deleteAll();
        proyectoRepository.deleteAll();

        Proyecto proyecto = Proyecto.builder().nombre("Edificio Aurora").build();
        proyectoRepository.save(proyecto);
        Torre torre = Torre.builder().nombre("Torre A").proyecto(proyecto).build();
        torreRepository.save(torre);
        piso = Piso.builder().nroPiso(1).torre(torre).build();
        pisoRepository.save(piso);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CP49 — Estructura física (torres, pisos, unidades).
    // El PDF exige explícitamente: "Eliminar Dpto 101 (vacío) → Valida que no tenga
    //   clientes/hitos activos antes de borrar".
    // ══════════════════════════════════════════════════════════════════════════

    // CP49 (verde): eliminar una unidad VACÍA (sin expediente) debe funcionar.
    @Test
    @CP(value = "CP49",
        scenario = "Eliminar una unidad vacia (sin clientes/hitos)",
        input = "DELETE /api/activos/{id} sobre Activo DISPONIBLE sin UsuarioActivo",
        expected = "204 + unidad eliminada de BD",
        type = CP.TestType.E2E)
    void cp49_eliminarUnidadVacia_funciona() throws Exception {
        Activo activo = nuevoActivo("A-101", EstadoComercialActivo.DISPONIBLE);

        mockMvc.perform(delete("/api/activos/{id}", activo.getId())
                        .with(securityContext(contextWithAuth()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(activoRepository.findById(activo.getId())).isEmpty();
    }

    // CP49 (DEFECTO): eliminar una unidad CON cliente/expediente activo DEBE rechazarse.
    //   El PDF exige "Valida que no tenga clientes/hitos activos antes de borrar".
    //   El backend (ActivoServiceImpl.deleteById) borra directo SIN validar => ROJO.
    @Disabled("DEFECTO reportado en Mantis (CP49): ActivoServiceImpl.deleteById() elimina la "
            + "unidad sin validar que no tenga clientes/hitos activos asociados. El PDF exige "
            + "rechazar el borrado de una unidad con expediente activo. REACTIVAR cuando se valide.")
    @Test
    @CP(value = "CP49",
        scenario = "Eliminar una unidad CON expediente activo debe ser rechazado",
        input = "DELETE /api/activos/{id} sobre Activo VENDIDO con UsuarioActivo",
        expected = "4xx (no se permite borrar una unidad con cliente/hito activo)",
        type = CP.TestType.E2E)
    void cp49_eliminarUnidadConExpediente_esRechazado() throws Exception {
        Activo activo = nuevoActivo("A-102", EstadoComercialActivo.VENDIDO);
        UsuarioActivo expediente = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .activos(List.of(activo))
                .build();
        usuarioActivoRepository.save(expediente);

        // El PDF exige rechazo; el sistema borra igual => este assert falla (rojo).
        mockMvc.perform(delete("/api/activos/{id}", activo.getId())
                        .with(securityContext(contextWithAuth()))
                        .with(csrf()))
                .andExpect(status().is4xxClientError());

        assertThat(activoRepository.findById(activo.getId())).isPresent();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CP51 — Transición de etapas comerciales.
    // El PDF exige: "SEPARACION → CONTRATO: Valida completitud de requisitos antes de
    //   transicionar" y "PAGO → ENTREGA: Verifica cuotas pagadas y financiamiento completo".
    // ══════════════════════════════════════════════════════════════════════════

    // CP51 (DEFECTO): transicionar la etapa a COMPLETADO sin requisitos completos
    //   DEBE rechazarse. El backend (EtapaExpedienteServiceImpl.actualizarEstado) solo
    //   hace setEstado() SIN validar requisitos ni cuotas => acepta cualquier transición.
    @Disabled("DEFECTO reportado en Mantis (CP51): EtapaExpedienteServiceImpl.actualizarEstado() "
            + "cambia el estado de la etapa sin validar la completitud de requisitos ni cuotas "
            + "pagadas, como exige el PDF. Permite COMPLETAR una etapa con requisitos pendientes. "
            + "REACTIVAR cuando se implemente la validacion de precondiciones de transicion.")
    @Test
    @CP(value = "CP51",
        scenario = "Transicionar etapa a COMPLETADO sin requisitos completos debe rechazarse",
        input = "PATCH /etapa-expediente/{uuid}/estado estado=COMPLETADO con requisitos PENDIENTES",
        expected = "4xx (valida completitud antes de transicionar)",
        type = CP.TestType.E2E)
    void cp51_transicionarSinRequisitos_esRechazado() throws Exception {
        UsuarioActivo expediente = UsuarioActivo.builder().tipoFinanciamiento("Credito Directo").build();
        usuarioActivoRepository.save(expediente);
        EtapaExpediente etapa = etapaExpedienteRepository.save(EtapaExpediente.builder()
                .usuarioActivo(expediente)
                .etapaProceso(EtapaProceso.SEPARACION)
                .estado(EstadoEtapaExpediente.PENDIENTE)
                .build());

        var req = new EtapaExpedienteEstadoRequest(EstadoEtapaExpediente.COMPLETADO);

        // El PDF exige validar completitud; el sistema acepta sin validar => rojo.
        mockMvc.perform(patch("/etapa-expediente/{uuid}/estado", etapa.getUuidEtapaExpediente())
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    // CP51 (verde, observación): el cambio de estado de la etapa SÍ persiste (la mecánica
    //   básica funciona; lo que falta es la validación de precondiciones, ver test rojo).
    @Test
    @CP(value = "CP51",
        scenario = "El cambio de estado de la etapa persiste correctamente",
        input = "PATCH /etapa-expediente/{uuid}/estado estado=COMPLETADO",
        expected = "200 + estado actualizado a COMPLETADO en BD",
        type = CP.TestType.E2E)
    void cp51_cambioDeEstadoEtapa_persiste() throws Exception {
        UsuarioActivo expediente = UsuarioActivo.builder().tipoFinanciamiento("Credito Directo").build();
        usuarioActivoRepository.save(expediente);
        EtapaExpediente etapa = etapaExpedienteRepository.save(EtapaExpediente.builder()
                .usuarioActivo(expediente)
                .etapaProceso(EtapaProceso.SEPARACION)
                .estado(EstadoEtapaExpediente.PENDIENTE)
                .build());

        var req = new EtapaExpedienteEstadoRequest(EstadoEtapaExpediente.COMPLETADO);

        mockMvc.perform(patch("/etapa-expediente/{uuid}/estado", etapa.getUuidEtapaExpediente())
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Activo nuevoActivo(String nro, EstadoComercialActivo estado) {
        Activo activo = Activo.builder()
                .nro(nro).tipo(TipoActivo.DEPARTAMENTO)
                .estadoComercial(estado)
                .piso(piso).build();
        return activoRepository.save(activo);
    }

    private SecurityContext contextWithAuth() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new FirebaseAuthenticationToken(
                "test-uid", "test@test.com",
                List.of(
                        new SimpleGrantedAuthority("PROY_VER"),
                        new SimpleGrantedAuthority("PROY_EDITAR"),
                        new SimpleGrantedAuthority("CONTRATO_EDITAR"),
                        new SimpleGrantedAuthority("CONTRATO_VER"))));
        return context;
    }
}
