package com.llosa.backend.proyecto.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.repository.HitoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.ReporteRepository;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E de flujo del módulo de Reportes (/api/reportes).
 *
 * Recorre el CRUD completo (crear, obtener, listar por proyecto, actualizar,
 * eliminar) para ejercer ReporteServiceImpl extremo a extremo. Tests de
 * integración (rol QA), no unitarios.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import({SecurityTestConfiguration.class, })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReporteFlowE2ETest {

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProyectoRepository proyectoRepository;
    @Autowired HitoRepository hitoRepository;
    @Autowired ReporteRepository reporteRepository;

    private Proyecto proyecto;

    @BeforeEach
    void setup() {
        reporteRepository.deleteAll();
        hitoRepository.deleteAll();
        proyectoRepository.deleteAll();

        proyecto = proyectoRepository.save(
                Proyecto.builder().nombre("Proy Reporte " + UUID.randomUUID()).build());
        // Dos hitos: uno completado, uno pendiente => avance 50%.
        hitoRepository.save(Hito.builder().orden(1).titulo("Cimentacion")
                .estado(EstadoHito.COMPLETADO).proyecto(proyecto).build());
        hitoRepository.save(Hito.builder().orden(2).titulo("Acabados")
                .estado(EstadoHito.PENDIENTE).proyecto(proyecto).build());
    }

    @Test
    void reporte_flujoCompletoCrud() throws Exception {
        // Generamos un token artificial limpio donde el principal es un Integer (1)
        // Extraemos los roles/authorities de tu método auth() original para no perder los permisos
        var authCorrecta = new TestingAuthenticationToken(1, null, auth().getAuthorities());
        authCorrecta.setAuthenticated(true);
        var contextoCorrecto = new SecurityContextImpl(authCorrecta);

        // ─── 1. CREAR ──────────────────────────────────────────────────────────
        String crearJson = """
        {
          "uuidProyecto": "%s",
          "tituloPeriodo": "Avance Enero 2026",
          "descripcion": "Reporte mensual",
          "fecha": "2026-01-31",
          "hitosConsolidados": ["Cimentacion"]
        }
        """.formatted(proyecto.getId());

        MockMultipartFile reportePart = new MockMultipartFile(
                "reporte",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                crearJson.getBytes()
        );

        // Ejecutamos usando el mé estándar .with(authentication(...))
        String creado = mockMvc.perform(multipart("/api/reportes")
                        .file(reportePart)
                        .with(authentication(authCorrecta)) // <-- ¡Úsalo así en todos los pasos del CRUD!
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.tituloPeriodo").value("Avance Enero 2026"))
                .andReturn().getResponse().getContentAsString();

        String reporteId = objectMapper.readTree(creado).get("id").asText();

        // ─── 2. OBTENER POR ID ──────────────────────────────────────────────────
        mockMvc.perform(get("/api/reportes/{id}", reporteId)
                        .with(securityContext(contextoCorrecto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reporteId));

        // ─── 3. LISTAR POR PROYECTO ─────────────────────────────────────────────
        mockMvc.perform(get("/api/reportes/proyecto/{uuid}", proyecto.getId())
                        .with(securityContext(contextoCorrecto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // ─── 4. ACTUALIZAR ──────────────────────────────────────────────────────
        String updateJson = """
        {
          "tituloPeriodo": "Avance Febrero 2026",
          "descripcion": "Reporte actualizado",
          "fecha": "2026-02-28",
          "hitosConsolidados": ["Cimentacion", "Acabados"]
        }
        """;
        mockMvc.perform(put("/api/reportes/{id}", reporteId)
                        .with(securityContext(contextoCorrecto))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tituloPeriodo").value("Avance Febrero 2026"));

        // ─── 5. ELIMINAR ────────────────────────────────────────────────────────
        mockMvc.perform(delete("/api/reportes/{id}", reporteId)
                        .with(securityContext(contextoCorrecto))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful());

        // ─── 6. OBTENER ELIMINADO ───────────────────────────────────────────────
        assertOperacionFalla(() -> mockMvc.perform(get("/api/reportes/{id}", reporteId)
                .with(securityContext(contextoCorrecto))));
    }

    @Test
    void reporte_crearConProyectoInexistente_devuelveError() {
        String crearJson = """
                {
                  "uuidProyecto": "%s",
                  "tituloPeriodo": "Sin proyecto",
                  "fecha": "2026-01-31"
                }
                """.formatted(UUID.randomUUID());

        // Proyecto inexistente => operacion falla (excepcion propagada o 4xx).
        assertOperacionFalla(() -> mockMvc.perform(post("/api/reportes")
                .with(securityContext(contextWithAuth(auth())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(crearJson)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Verifica que una operacion NO tenga exito: o responde con status >= 400, o
     * propaga una excepcion (EntityNotFoundException no esta mapeada a HTTP).
     */
    private static void assertOperacionFalla(ThrowingPerform op) {
        boolean fallo;
        try {
            int status = op.run().andReturn().getResponse().getStatus();
            fallo = status >= 400;
        } catch (Exception e) {
            fallo = true;
        }
        org.assertj.core.api.Assertions.assertThat(fallo)
                .as("la operacion deberia fallar").isTrue();
    }

    @FunctionalInterface
    private interface ThrowingPerform {
        org.springframework.test.web.servlet.ResultActions run() throws Exception;
    }

    private static FirebaseAuthenticationToken auth() {
        return new FirebaseAuthenticationToken(
                "reporte-uid", "user@utec.edu.pe",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("PROY_VER"),
                        new SimpleGrantedAuthority("PROY_EDITAR")));
    }

    private static SecurityContext contextWithAuth(FirebaseAuthenticationToken auth) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
