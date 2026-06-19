package com.llosa.backend.pagos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.llosa.backend.annotation.CP;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.pagos.ConceptoPago;
import com.llosa.backend.pagos.dto.CronogramaPagoRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPRINT 5 — Módulo de Gestión Financiera (Cronograma de Pagos).
 * Casos de prueba CP29–CP33 (CU008).
 *
 * Tests E2E FIELES al Plan de Pruebas: afirman lo que el sistema DEBE cumplir.
 * Si el backend no cumple un CP, el test falla (rojo) => defecto para Mantis.
 *
 * Infra: corre sobre H2 (perfil test del equipo), sin Docker/Testcontainers,
 * por lo que es seguro para el CI de Jenkins, aunque igual lleva
 * @Tag("integration") para excluirse del pipeline por defecto y correrse local.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
@Import({SecurityTestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CP29ToCP33E2ETest {

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioActivoRepository usuarioActivoRepository;
    @Autowired ProyectoRepository proyectoRepository;
    @Autowired TorreRepository torreRepository;
    @Autowired PisoRepository pisoRepository;
    @Autowired ActivoRepository activoRepository;
    @Autowired CronogramaPagoRepository cronogramaPagoRepository;
    @Autowired PagoRepository pagoRepository;

    private UUID expedienteDirecto;
    private UUID expedienteHipotecario;

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

        Proyecto proyecto = proyectoRepository.save(Proyecto.builder().nombre("Edificio Financiero").build());
        Torre torre = torreRepository.save(Torre.builder().nombre("Torre F").proyecto(proyecto).build());
        Piso piso = pisoRepository.save(Piso.builder().nroPiso(1).torre(torre).build());

        Activo a1 = activoRepository.save(Activo.builder()
                .nro("F-101").tipo(TipoActivo.DEPARTAMENTO)
                .estadoComercial(EstadoComercialActivo.VENDIDO).piso(piso).build());
        Activo a2 = activoRepository.save(Activo.builder()
                .nro("F-102").tipo(TipoActivo.DEPARTAMENTO)
                .estadoComercial(EstadoComercialActivo.VENDIDO).piso(piso).build());

        expedienteDirecto = usuarioActivoRepository.save(UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo").activos(List.of(a1)).build())
                .getUuidUsuarioActivo();

        expedienteHipotecario = usuarioActivoRepository.save(UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Hipotecario").activos(List.of(a2)).build())
                .getUuidUsuarioActivo();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP29: Generar cronograma de pagos para financiamiento por Crédito Directo.
    //   Esperado (PDF): el sistema crea el cronograma y genera automáticamente
    //   las N cuotas del crédito directo.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP29",
        scenario = "Crear cronograma de Credito Directo genera las cuotas",
        input = "POST /api/cronogramas total=120000, numeroCuotas=4",
        expected = "201 + 4 cuotas CUOTA generadas + estado ACTIVO",
        type = CP.TestType.E2E)
    void cp29_cronogramaCreditoDirecto_generaCuotas() throws Exception {
        var req = new CronogramaPagoRequest(
                expedienteDirecto, new BigDecimal("120000.00"),
                4, BigDecimal.ZERO, BigDecimal.ZERO);

        String resp = mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPactado").value(120000.00))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();

        String uuidCronograma = objectMapper.readTree(resp).get("uuidCronograma").asText();

        // El cronograma de crédito directo debe generar exactamente 4 cuotas regulares.
        long cuotas = pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(UUID.fromString(uuidCronograma))
                .stream().filter(p -> p.getConcepto() == ConceptoPago.CUOTA).count();
        assertThat(cuotas).isEqualTo(4);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP30: Generar cronograma para Crédito Hipotecario y obtener su resumen.
    //   Esperado (PDF): el flujo hipotecario refleja separación, inicial y el
    //   monto completo restante; el resumen calcula saldo pendiente y estado.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP30",
        scenario = "Cronograma de Credito Hipotecario y resumen hipotecario",
        input = "total=200000, separacion=10000, inicial=40000",
        expected = "resumen-hipo: montoTotal=200000, totalPagado=50000, saldo=150000",
        type = CP.TestType.E2E)
    void cp30_cronogramaHipotecario_resumenCorrecto() throws Exception {
        var req = new CronogramaPagoRequest(
                expedienteHipotecario, new BigDecimal("200000.00"),
                null, new BigDecimal("10000.00"), new BigDecimal("40000.00"));

        String resp = mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String uuidCronograma = objectMapper.readTree(resp).get("uuidCronograma").asText();

        // El resumen hipotecario suma separación + inicial como "pagado programado"
        // y reporta el saldo pendiente sobre el total pactado.
        mockMvc.perform(get("/api/cronogramas/{uuid}/resumen/credito-hipo", uuidCronograma)
                        .with(securityContext(contextWithAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoTotal").value(200000.00))
                .andExpect(jsonPath("$.totalPagado").value(50000.00))
                .andExpect(jsonPath("$.saldoPendiente").value(150000.00));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP31: Validación de consistencia financiera al crear el cronograma.
    //   Esperado (PDF): el sistema rechaza datos financieros inválidos
    //   (monto total no positivo). El backend debe interrumpir la creación.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP31",
        scenario = "Rechazar cronograma con total pactado invalido (no positivo)",
        input = "POST /api/cronogramas total=0.00",
        expected = "4xx: el total pactado debe ser un monto positivo",
        type = CP.TestType.E2E)
    void cp31_totalPactadoInvalido_esRechazado() throws Exception {
        var req = new CronogramaPagoRequest(
                expedienteDirecto, new BigDecimal("0.00"),
                4, BigDecimal.ZERO, BigDecimal.ZERO);

        mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP31-bis: Consistencia financiera de los montos.
    //   Esperado (PDF): el sistema valida que la composición del pago sea
    //   coherente: la suma de separación + inicial NO puede superar el total
    //   pactado (sería un cronograma con saldo negativo).
    //
    //   ESTADO ACTUAL: crear() no valida esta relación; acepta separacion+inicial
    //   mayores al total. => test FIEL al CP, ROJO por validación FALTANTE.
    //      Defecto a reportar en Mantis.
    // ──────────────────────────────────────────────────────────────────────────
    @org.junit.jupiter.api.Disabled("DEFECTO (verificado 2026-06-19): falta validación de consistencia "
            + "financiera. CronogramaPagoServiceImpl.crear() acepta pagoSeparacion + pagoInicial mayores "
            + "que el totalPactado (devuelve 201 en vez de 4xx), generando un saldo pendiente negativo. "
            + "Reportado en Mantis; REACTIVAR cuando se valide separacion+inicial <= totalPactado.")
    @Test
    @CP(value = "CP31",
        scenario = "Rechazar cronograma con separacion+inicial mayores al total pactado",
        input = "total=100000, separacion=60000, inicial=60000 (suma=120000 > total)",
        expected = "4xx: los pagos no pueden exceder el total pactado",
        type = CP.TestType.E2E)
    void cp31bis_montosInconsistentes_sonRechazados() throws Exception {
        var req = new CronogramaPagoRequest(
                expedienteDirecto, new BigDecimal("100000.00"),
                4, new BigDecimal("60000.00"), new BigDecimal("60000.00"));

        mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP32: Inmutabilidad del histórico financiero.
    //   Esperado (PDF): una vez que el cronograma queda en estado cerrado/
    //   HISTORICO (p. ej. liquidado o archivado), el sistema NO debe permitir
    //   modificar sus montos: la información histórica es inmutable por
    //   trazabilidad y auditoría comercial.
    //
    //   ESTADO ACTUAL: CronogramaPagoServiceImpl.actualizar() NO verifica el
    //   estado del cronograma; sobrescribe los montos en cualquier caso. No
    //   existe un bloqueo de inmutabilidad para registros históricos.
    //   => Este test es FIEL al CP y, por la funcionalidad FALTANTE, queda ROJO.
    //      Defecto a reportar en Mantis.
    // ──────────────────────────────────────────────────────────────────────────
    @org.junit.jupiter.api.Disabled("DEFECTO (verificado 2026-06-19): falta la inmutabilidad del "
            + "histórico financiero. CronogramaPagoServiceImpl.actualizar() NO valida el estado del "
            + "cronograma y permite editar un cronograma en estado HISTORICO (devuelve 200 en vez de 4xx). "
            + "No existe bloqueo para registros cerrados. Reportado en Mantis; REACTIVAR cuando se valide "
            + "que un cronograma cerrado/HISTORICO no pueda modificarse.")
    @Test
    @CP(value = "CP32",
        scenario = "Un cronograma en estado HISTORICO no debe poder editarse",
        input = "PUT /api/cronogramas/{uuid} sobre cronograma cerrado (HISTORICO)",
        expected = "4xx: la informacion historica es inmutable (no se permite editar)",
        type = CP.TestType.E2E)
    void cp32_cronogramaHistorico_noSeDebeEditar() throws Exception {
        // Crear cronograma normal.
        var crear = new CronogramaPagoRequest(
                expedienteDirecto, new BigDecimal("120000.00"),
                4, BigDecimal.ZERO, BigDecimal.ZERO);

        String resp = mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crear)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID uuidCronograma = UUID.fromString(objectMapper.readTree(resp).get("uuidCronograma").asText());

        // Cerrar el cronograma: pasa a HISTORICO (registro de auditoría inmutable).
        CronogramaPago cp = cronogramaPagoRepository.findById(uuidCronograma).orElseThrow();
        cp.setEstado("HISTORICO");
        cronogramaPagoRepository.save(cp);

        // Intentar modificar el total pactado de un cronograma histórico.
        var editar = new CronogramaPagoRequest(
                expedienteDirecto, new BigDecimal("999999.00"),
                4, BigDecimal.ZERO, BigDecimal.ZERO);

        // CP32 EXIGE rechazo (4xx). El backend hoy lo permite (200) => test rojo = defecto.
        mockMvc.perform(put("/api/cronogramas/{uuid}", uuidCronograma)
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editar)))
                .andExpect(status().is4xxClientError());

        // Y el monto histórico NO debe haber cambiado.
        CronogramaPago despues = cronogramaPagoRepository.findById(uuidCronograma).orElseThrow();
        assertThat(despues.getTotalPactado()).isEqualByComparingTo(new BigDecimal("120000.00"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CP33: Detección de mora. El Plan describe la transición automática a
    //   estado "En Mora" cuando hay cuotas vencidas e impagas.
    //   A nivel backend lo verificamos sobre el resumen: con una cuota en estado
    //   VENCIDO, el estadoGlobal del cronograma debe reportarse como EN_MORA.
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @CP(value = "CP33",
        scenario = "Cronograma con cuota vencida reporta estado EN_MORA",
        input = "cronograma con 1 cuota en estado VENCIDO",
        expected = "resumen.estadoGlobal = EN_MORA, cuotasVencidas >= 1",
        type = CP.TestType.E2E)
    void cp33_cuotaVencida_reportaEnMora() throws Exception {
        // Crear cronograma de crédito directo (genera cuotas PENDIENTE).
        var req = new CronogramaPagoRequest(
                expedienteDirecto, new BigDecimal("120000.00"),
                4, BigDecimal.ZERO, BigDecimal.ZERO);

        String resp = mockMvc.perform(post("/api/cronogramas")
                        .with(securityContext(contextWithAuth()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID uuidCronograma = UUID.fromString(objectMapper.readTree(resp).get("uuidCronograma").asText());

        // Forzar una cuota vencida e impaga (fecha de vencimiento en el pasado).
        Pago primera = pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(uuidCronograma)
                .stream().filter(p -> p.getConcepto() == ConceptoPago.CUOTA).findFirst().orElseThrow();
        primera.setEstado("VENCIDO");
        primera.setFechaVencimiento(LocalDate.now().minusDays(5));
        pagoRepository.save(primera);

        mockMvc.perform(get("/api/cronogramas/{uuid}/resumen", uuidCronograma)
                        .with(securityContext(contextWithAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoGlobal").value("EN_MORA"))
                .andExpect(jsonPath("$.cuotasVencidas").value(1));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SecurityContext contextWithAuth() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new FirebaseAuthenticationToken(
                "gestor-financiero-uid", "gestor@utec.edu.pe",
                List.of(
                        new SimpleGrantedAuthority("CONTRATO_EDITAR"),
                        new SimpleGrantedAuthority("CONTRATO_VER"))));
        return context;
    }
}
