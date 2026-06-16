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
    void flujoCompleto_cronogramaConPagos_resumenCorrecto() throws Exception {
        // 1. Crear cronograma
        var crearRequest = new CronogramaPagoRequest(
                uuidExpediente, new BigDecimal("100000.00"),
                new BigDecimal("20000.00"), 4,
                BigDecimal.ZERO, BigDecimal.ZERO);

        mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPactado").value(100000.00))
                .andExpect(jsonPath("$.cuotaInicial").value(20000.00))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        // 2. Obtener cronograma por expediente
        String getResponse = mockMvc.perform(get("/api/cronogramas/{uuidUsuarioActivo}", uuidExpediente)
                        .with(securityContext(contextWithAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPactado").value(100000.00))
                .andReturn().getResponse().getContentAsString();

        String uuidCronograma = objectMapper.readTree(getResponse).get("uuidCronograma").asText();

        // 3. Agregar cuotas
        var pago1 = new PagoRequest(1, new BigDecimal("20000.00"), LocalDate.now().plusMonths(1));
        var pago2 = new PagoRequest(2, new BigDecimal("20000.00"), LocalDate.now().plusMonths(2));
        var pago3 = new PagoRequest(3, new BigDecimal("20000.00"), LocalDate.now().plusMonths(3));
        var pago4 = new PagoRequest(4, new BigDecimal("20000.00"), LocalDate.now().plusMonths(4));

        mockMvc.perform(post("/api/cronogramas/{uuidCronograma}/pagos", uuidCronograma)
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pago1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cronogramas/{uuidCronograma}/pagos", uuidCronograma)
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pago2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cronogramas/{uuidCronograma}/pagos", uuidCronograma)
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pago3)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cronogramas/{uuidCronograma}/pagos", uuidCronograma)
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pago4)))
                .andExpect(status().isCreated());

        // 4. Listar pagos del cronograma
        mockMvc.perform(get("/api/cronogramas/{uuidCronograma}/pagos", uuidCronograma)
                        .with(securityContext(contextWithAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        // 5. Obtener resumen (todo pendiente, al dia)
        mockMvc.perform(get("/api/cronogramas/{uuidCronograma}/resumen", uuidCronograma)
                        .with(securityContext(contextWithAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPactado").value(100000.00))
                .andExpect(jsonPath("$.totalPagado").value(0.00))
                .andExpect(jsonPath("$.totalPendiente").value(100000.00))
                .andExpect(jsonPath("$.cuotasPagadas").value(0))
                .andExpect(jsonPath("$.cuotasPendientes").value(4))
                .andExpect(jsonPath("$.estadoGlobal").value("AL_DIA"));

        // 6. Verificar en BD
        CronogramaPago cp = cronogramaPagoRepository.findById(UUID.fromString(uuidCronograma)).orElseThrow();
        assertThat(cp.getTotalPactado()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(pagoRepository.countByCronograma_IdAndEstado(cp.getId(), "PENDIENTE")).isEqualTo(4);
    }

    @Test
    void crearCronogramaDuplicado_devuelve409() throws Exception {
        var request = new CronogramaPagoRequest(
                uuidExpediente, new BigDecimal("100000.00"), null, 4,
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
                uuidExpediente, new BigDecimal("100000.00"), null, 4,
                BigDecimal.ZERO, BigDecimal.ZERO);

        String response = mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cronogramaRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String uuidCp = objectMapper.readTree(response).get("uuidCronograma").asText();

        var pagoReq = new PagoRequest(1, new BigDecimal("25000.00"), LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/cronogramas/{uuidCronograma}/pagos", uuidCp)
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pagoReq)))
                .andExpect(status().isCreated());

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
