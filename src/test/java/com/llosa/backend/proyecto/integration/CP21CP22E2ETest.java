package com.llosa.backend.proyecto.integration;

import com.llosa.backend.annotation.CP;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.GcsTestConfig;
import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.enums.TipoHito;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.PisoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.TorreRepository;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPRINT 3 — Tracker de Avance de Obra.
 * Casos de prueba CP21 (actualización de avance) y CP22 (precedencia de hitos).
 *
 * NOTA DE ALCANCE: el endpoint de avance (PUT /api/avances-unidad/{id}) cambia el
 * estado del hito por JSON; NO recibe multimedia. La carga de fotos/videos a GCS
 * que describe el PDF para CP21/CP23/CP24 NO existe en el módulo de Obra (la
 * gestión multimedia/GCS vive en el módulo de Documentos / Bóveda). Por eso aquí
 * se valida lo que el backend de avance SÍ hace: cambio de estado + precedencia.
 *
 * Tests FIELES al comportamiento exigido: si el backend no cumple, fallan (Mantis).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class, SecurityTestConfiguration.class, GcsTestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CP21CP22E2ETest {

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @Autowired MockMvc mockMvc;
    @Autowired ProyectoRepository proyectoRepository;
    @Autowired TorreRepository torreRepository;
    @Autowired PisoRepository pisoRepository;
    @Autowired HitoRepository hitoRepository;
    @Autowired HitoPisoRepository hitoPisoRepository;

    private Piso piso;

    @BeforeEach
    void setup() {
        hitoPisoRepository.deleteAll();
        hitoRepository.deleteAll();
        pisoRepository.deleteAll();
        torreRepository.deleteAll();
        proyectoRepository.deleteAll();

        Proyecto proyecto = proyectoRepository.save(
                Proyecto.builder().nombre("Proy Obra " + UUID.randomUUID()).build());
        Torre torre = torreRepository.save(
                Torre.builder().nombre("Torre A").proyecto(proyecto).build());
        piso = pisoRepository.save(Piso.builder().nroPiso(5).torre(torre).build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP21: Actualización de un hito constructivo (avance válido).
    //   Esperado (PDF, parte aplicable al backend de obra): valida regla de
    //   precedencia (hito anterior OK) y marca el hito como Completado,
    //   recalculando el avance.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP21",
        scenario = "Completar hito con precedencia satisfecha",
        input = "hito anterior COMPLETADO; PUT /api/avances-unidad/{id} estado=COMPLETADO",
        expected = "2xx + hito queda COMPLETADO",
        type = CP.TestType.E2E)
    void cp21_completarHito_conPrecedenciaOk() throws Exception {
        Hito hito1 = crearHito(1, "Cimentacion");
        Hito hito2 = crearHito(2, "Acabados");

        // hito1 ya completado (precedencia satisfecha)
        crearHitoPiso(hito1, EstadoHito.COMPLETADO);
        HitoPiso hp2 = crearHitoPiso(hito2, EstadoHito.PENDIENTE);

        mockMvc.perform(put("/api/avances-unidad/{id}", hp2.getId())
                        .with(securityContext(contextWithAuth(obraAuth())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"COMPLETADO\"}"))
                .andExpect(status().is2xxSuccessful());

        HitoPiso actualizado = hitoPisoRepository.findById(hp2.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoHito.COMPLETADO);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP22: Bloqueo por violación de la regla de precedencia.
    //   Esperado (PDF): detecta hito predecesor 'Pendiente', bloquea la acción y
    //   descarta la carga (no permite completar fuera de orden).
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP22",
        scenario = "Completar hito con predecesor PENDIENTE debe bloquearse",
        input = "hito anterior PENDIENTE; PUT estado=COMPLETADO",
        expected = "error (4xx/5xx) + hito NO queda COMPLETADO",
        type = CP.TestType.E2E)
    void cp22_precedenciaViolada_seBloquea() throws Exception {
        Hito hito1 = crearHito(1, "Cimentacion");
        Hito hito2 = crearHito(2, "Acabados");

        // hito1 PENDIENTE => no se puede completar hito2
        crearHitoPiso(hito1, EstadoHito.PENDIENTE);
        HitoPiso hp2 = crearHitoPiso(hito2, EstadoHito.PENDIENTE);

        // Lo esencial de CP22: la accion se BLOQUEA y el hito NO queda completado.
        // El backend bloquea con BusinessException (regla de precedencia correcta).
        // NOTA/DEFECTO MENOR a reportar en Mantis: BusinessException NO esta mapeada
        // en GlobalExceptionHandler, por lo que NO se traduce a un HTTP 4xx limpio
        // (se propaga). Aqui verificamos el bloqueo efectivo: la peticion no tiene
        // exito (sea por status >=400 o por excepcion propagada).
        boolean bloqueado;
        try {
            int status = mockMvc.perform(put("/api/avances-unidad/{id}", hp2.getId())
                            .with(securityContext(contextWithAuth(obraAuth())))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\":\"COMPLETADO\"}"))
                    .andReturn().getResponse().getStatus();
            bloqueado = status >= 400;
        } catch (Exception propagada) {
            // BusinessException relanzada por MockMvc => tambien es un bloqueo.
            bloqueado = true;
        }
        assertThat(bloqueado).as("CP22: la accion debe bloquearse por precedencia").isTrue();

        // El hito NO debe haber quedado completado (precedencia respetada).
        HitoPiso actualizado = hitoPisoRepository.findById(hp2.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isNotEqualTo(EstadoHito.COMPLETADO);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Hito crearHito(int orden, String titulo) {
        return hitoRepository.save(Hito.builder()
                .orden(orden)
                .tipo(TipoHito.OBRA)
                .titulo(titulo)
                .estado(EstadoHito.PENDIENTE)
                .proyecto(piso.getTorre().getProyecto())
                .build());
    }

    private HitoPiso crearHitoPiso(Hito hito, EstadoHito estado) {
        return hitoPisoRepository.save(HitoPiso.builder()
                .estado(estado)
                .piso(piso)
                .hito(hito)
                .build());
    }

    private static FirebaseAuthenticationToken obraAuth() {
        return new FirebaseAuthenticationToken(
                "obra-uid", "obra@utec.edu.pe",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("OBRA_VER"),
                        new SimpleGrantedAuthority("OBRA_EDITAR")));
    }

    private static SecurityContext contextWithAuth(FirebaseAuthenticationToken auth) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
