package com.llosa.backend.pagos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.PostgresTestContainerConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.pagos.dto.CronogramaPagoRequest;
import com.llosa.backend.pagos.dto.PagoRequest;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.pagos.entity.Pago;
import com.llosa.backend.pagos.repository.CronogramaPagoRepository;
import com.llosa.backend.pagos.repository.PagoRepository;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
@Import({SecurityTestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PagosIntegrationTest {

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UsuarioActivoRepository usuarioActivoRepository;

    @Autowired
    ProyectoRepository proyectoRepository;

    @Autowired
    TorreRepository torreRepository;

    @Autowired
    PisoRepository pisoRepository;

    @Autowired
    ActivoRepository activoRepository;

    @Autowired
    CronogramaPagoRepository cronogramaPagoRepository;

    @Autowired
    PagoRepository pagoRepository;

    private UsuarioActivo expediente;
    private UUID uuidExpediente;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        pagoRepository.deleteAll();
        cronogramaPagoRepository.deleteAll();
        usuarioActivoRepository.deleteAll();
        activoRepository.deleteAll();
        pisoRepository.deleteAll();
        torreRepository.deleteAll();
        proyectoRepository.deleteAll();

        Proyecto proyecto = Proyecto.builder().nombre("Test Proyecto").build();
        proyectoRepository.save(proyecto);

        Torre torre = Torre.builder().nombre("Torre A").proyecto(proyecto).build();
        torreRepository.save(torre);

        Piso piso = Piso.builder().nroPiso(1).torre(torre).build();
        pisoRepository.save(piso);

        Activo activo = Activo.builder()
                .nro("A-101").tipo(TipoActivo.DEPARTAMENTO)
                .estadoComercial(EstadoComercialActivo.VENDIDO)
                .piso(piso).build();
        activoRepository.save(activo);

        expediente = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .activos(List.of(activo))
                .build();
        usuarioActivoRepository.save(expediente);
        uuidExpediente = expediente.getUuidUsuarioActivo();
    }

    @Test
    void flujoCompleto_cronogramaConPagosAutoGenerados_resumenCorrecto() throws Exception {
        // 1. Crear cronograma — genera automáticamente:
        //    SEPARACION(-1, monto=0), INICIAL(0, monto=0), CUOTA(1..4, monto=25000)
        var crearRequest = new CronogramaPagoRequest(
                uuidExpediente, new BigDecimal("100000.00"),
                4, BigDecimal.ZERO, BigDecimal.ZERO);

        String createResponse = mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPactado").value(100000.00))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();

        String uuidCronograma = objectMapper.readTree(createResponse).get("uuidCronograma").asText();

        // 2. Listar pagos auto-generados (6: SEPARACION + INICIAL + 4 CUOTAS)
        mockMvc.perform(get("/api/cronogramas/{uuidCronograma}/pagos", uuidCronograma)
                        .with(securityContext(contextWithAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));

        // 3. Obtener resumen (todo pendiente)
        mockMvc.perform(get("/api/cronogramas/{uuidCronograma}/resumen", uuidCronograma)
                        .with(securityContext(contextWithAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPactado").value(100000.00))
                .andExpect(jsonPath("$.totalPagado").value(0.00))
                .andExpect(jsonPath("$.totalPendiente").value(100000.00))
                .andExpect(jsonPath("$.cuotasPagadas").value(0))
                .andExpect(jsonPath("$.cuotasPendientes").value(6))
                .andExpect(jsonPath("$.estadoGlobal").value("AL_DIA"));

        // 4. Verificar en BD
        CronogramaPago cp = cronogramaPagoRepository.findById(UUID.fromString(uuidCronograma)).orElseThrow();
        assertThat(cp.getTotalPactado()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(pagoRepository.countByCronograma_IdAndEstado(cp.getId(), "PENDIENTE")).isEqualTo(6);
    }

    @Test
    void crearCronogramaDuplicado_devuelve409() throws Exception {
        var request = new CronogramaPagoRequest(
                uuidExpediente, new BigDecimal("100000.00"), 4,
                BigDecimal.ZERO, BigDecimal.ZERO);

        mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void agregarCuotaDuplicada_devuelve409() throws Exception {
        var cronogramaRequest = new CronogramaPagoRequest(
                uuidExpediente, new BigDecimal("100000.00"), 4,
                BigDecimal.ZERO, BigDecimal.ZERO);

        String response = mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cronogramaRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String uuidCp = objectMapper.readTree(response).get("uuidCronograma").asText();

        // Las cuotas 1..4 ya fueron auto-generadas, agregar cuota 5 debería funcionar
        var pagoReq = new PagoRequest(5, new BigDecimal("25000.00"), LocalDate.now().plusMonths(5), com.llosa.backend.pagos.ConceptoPago.CUOTA, null);

        mockMvc.perform(post("/api/cronogramas/{uuidCronograma}/pagos", uuidCp)
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pagoReq)))
                .andExpect(status().isCreated());

        // Duplicar nroCuota=5 debe dar 409
        mockMvc.perform(post("/api/cronogramas/{uuidCronograma}/pagos", uuidCp)
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pagoReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    private SecurityContext contextWithAuth() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new FirebaseAuthenticationToken(
                "test-uid", "test@test.com",
                List.of(
                        new SimpleGrantedAuthority("CONTRATO_EDITAR"),
                        new SimpleGrantedAuthority("CONTRATO_VER"),
                        new SimpleGrantedAuthority("PAGO_EDITAR"),
                        new SimpleGrantedAuthority("PAGO_VER")
                )));
        return context;
    }
}
