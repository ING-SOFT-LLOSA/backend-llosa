package com.llosa.backend.pagos.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.llosa.backend.config.TestDataPagos;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.pagos.dto.CronogramaPagoRequest;
import com.llosa.backend.pagos.dto.CronogramaPagoResponse;
import com.llosa.backend.pagos.dto.ResumenResponse;
import com.llosa.backend.pagos.service.CronogramaPagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CronogramaPagoControllerTest {

    private MockMvc mockMvc;

    @Mock
    CronogramaPagoService cronogramaPagoService;

    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CronogramaPagoController(cronogramaPagoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void crear_conDatosValidos_devuelve201() throws Exception {
        var request = TestDataPagos.crearCronogramaRequest();
        var response = new CronogramaPagoResponse(
                UUID.randomUUID(), request.uuidUsuarioActivo(),
                request.totalPactado(), request.cuotaInicial(),
                request.numeroCuotas(), "ACTIVO", null, null);

        when(cronogramaPagoService.crear(any(CronogramaPagoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/cronogramas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPactado").value(350000.00))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crear_expedienteDuplicado_devuelve400() throws Exception {
        when(cronogramaPagoService.crear(any(CronogramaPagoRequest.class)))
                .thenThrow(new BusinessException("El expediente ya tiene un cronograma de pagos activo"));

        mockMvc.perform(post("/api/cronogramas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataPagos.crearCronogramaRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El expediente ya tiene un cronograma de pagos activo"));
    }

    @Test
    void crear_expedienteNoExiste_devuelve404() throws Exception {
        when(cronogramaPagoService.crear(any(CronogramaPagoRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Expediente no encontrado"));

        mockMvc.perform(post("/api/cronogramas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataPagos.crearCronogramaRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerPorExpediente_exitoso_devuelve200() throws Exception {
        UUID uuidUa = UUID.randomUUID();
        var response = new CronogramaPagoResponse(
                UUID.randomUUID(), uuidUa,
                new BigDecimal("350000.00"), new BigDecimal("50000.00"),
                12, "ACTIVO", null, null);

        when(cronogramaPagoService.obtenerPorUsuarioActivo(uuidUa)).thenReturn(response);

        mockMvc.perform(get("/api/cronogramas/{uuidUsuarioActivo}", uuidUa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuidUsuarioActivo").value(uuidUa.toString()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void obtenerPorExpediente_noExiste_devuelve404() throws Exception {
        UUID uuidUa = UUID.randomUUID();
        when(cronogramaPagoService.obtenerPorUsuarioActivo(uuidUa))
                .thenThrow(new RecursoNoEncontradoException("No hay cronograma"));

        mockMvc.perform(get("/api/cronogramas/{uuidUsuarioActivo}", uuidUa))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_exitoso_devuelve200() throws Exception {
        UUID uuidCp = UUID.randomUUID();
        var request = TestDataPagos.crearCronogramaRequest();
        var response = new CronogramaPagoResponse(
                uuidCp, request.uuidUsuarioActivo(),
                request.totalPactado(), request.cuotaInicial(),
                request.numeroCuotas(), "ACTIVO", null, null);

        when(cronogramaPagoService.actualizar(eq(uuidCp), any(CronogramaPagoRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/cronogramas/{uuidCronograma}", uuidCp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuidCronograma").value(uuidCp.toString()));
    }

    @Test
    void eliminar_exitoso_devuelve204() throws Exception {
        UUID uuidCp = UUID.randomUUID();

        mockMvc.perform(delete("/api/cronogramas/{uuidCronograma}", uuidCp))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_noExiste_devuelve404() throws Exception {
        UUID uuidCp = UUID.randomUUID();
        doThrow(new RecursoNoEncontradoException("Cronograma no encontrado"))
                .when(cronogramaPagoService).eliminar(uuidCp);

        mockMvc.perform(delete("/api/cronogramas/{uuidCronograma}", uuidCp))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerResumen_exitoso_devuelve200() throws Exception {
        UUID uuidCp = UUID.randomUUID();
        var resumen = new ResumenResponse(
                new BigDecimal("350000.00"), new BigDecimal("100000.00"),
                new BigDecimal("250000.00"), "AL_DIA",
                4, 8, 0, LocalDate.now().plusMonths(1));

        when(cronogramaPagoService.obtenerResumen(uuidCp)).thenReturn(resumen);

        mockMvc.perform(get("/api/cronogramas/{uuidCronograma}/resumen", uuidCp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPactado").value(350000.00))
                .andExpect(jsonPath("$.totalPagado").value(100000.00))
                .andExpect(jsonPath("$.estadoGlobal").value("AL_DIA"));
    }

    @Test
    void obtenerResumen_cronogramaNoExiste_devuelve404() throws Exception {
        UUID uuidCp = UUID.randomUUID();
        when(cronogramaPagoService.obtenerResumen(uuidCp))
                .thenThrow(new RecursoNoEncontradoException("Cronograma no encontrado"));

        mockMvc.perform(get("/api/cronogramas/{uuidCronograma}/resumen", uuidCp))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_sinBody_devuelve400() throws Exception {
        mockMvc.perform(post("/api/cronogramas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
