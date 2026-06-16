package com.llosa.backend.pagos.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.llosa.backend.config.TestDataPagos;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.EntidadDuplicadaException;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.pagos.dto.CartaAprobacionRequest;
import com.llosa.backend.pagos.dto.CartaAprobacionResponse;
import com.llosa.backend.pagos.service.CartaAprobacionService;
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
class CartaAprobacionControllerTest {

    private MockMvc mockMvc;

    @Mock
    CartaAprobacionService cartaAprobacionService;

    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CartaAprobacionController(cartaAprobacionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void crear_conDatosValidos_devuelve201() throws Exception {
        var request = TestDataPagos.crearCartaRequest();
        var response = new CartaAprobacionResponse(
                UUID.randomUUID(), request.uuidUsuarioActivo(),
                request.banco(), request.montoAprobado(),
                request.fechaEmision(), request.fechaVencimiento(),
                request.fechaDesembolsoProyectada(), request.comentarios(), null);

        when(cartaAprobacionService.crear(any(CartaAprobacionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/cartas-aprobacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.banco").value("Banco de Prueba"))
                .andExpect(jsonPath("$.montoAprobado").value(300000.00));
    }

    @Test
    void crear_duplicado_devuelve409() throws Exception {
        when(cartaAprobacionService.crear(any(CartaAprobacionRequest.class)))
                .thenThrow(new EntidadDuplicadaException("El expediente ya tiene una carta de aprobación registrada"));

        mockMvc.perform(post("/api/cartas-aprobacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataPagos.crearCartaRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void crear_expedienteNoExiste_devuelve404() throws Exception {
        when(cartaAprobacionService.crear(any(CartaAprobacionRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Expediente no encontrado"));

        mockMvc.perform(post("/api/cartas-aprobacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataPagos.crearCartaRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerPorExpediente_exitoso_devuelve200() throws Exception {
        UUID uuidUa = UUID.randomUUID();
        var response = new CartaAprobacionResponse(
                UUID.randomUUID(), uuidUa,
                "Banco de Prueba", new BigDecimal("300000.00"),
                LocalDate.now(), LocalDate.now().plusMonths(6),
                LocalDate.now().plusMonths(1), "Comentarios", null);

        when(cartaAprobacionService.obtenerPorUsuarioActivo(uuidUa)).thenReturn(response);

        mockMvc.perform(get("/api/cartas-aprobacion/{uuidUsuarioActivo}", uuidUa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banco").value("Banco de Prueba"))
                .andExpect(jsonPath("$.montoAprobado").value(300000.00));
    }

    @Test
    void obtenerPorExpediente_noExiste_devuelve404() throws Exception {
        UUID uuidUa = UUID.randomUUID();
        when(cartaAprobacionService.obtenerPorUsuarioActivo(uuidUa))
                .thenThrow(new RecursoNoEncontradoException("No hay carta de aprobación"));

        mockMvc.perform(get("/api/cartas-aprobacion/{uuidUsuarioActivo}", uuidUa))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_exitoso_devuelve200() throws Exception {
        UUID uuidCarta = UUID.randomUUID();
        var request = TestDataPagos.crearCartaRequest();
        var response = new CartaAprobacionResponse(
                uuidCarta, request.uuidUsuarioActivo(),
                "Banco Actualizado", request.montoAprobado(),
                request.fechaEmision(), request.fechaVencimiento(),
                request.fechaDesembolsoProyectada(), request.comentarios(), null);

        when(cartaAprobacionService.actualizar(eq(uuidCarta), any(CartaAprobacionRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/cartas-aprobacion/{uuidCarta}", uuidCarta)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banco").value("Banco Actualizado"));
    }

    @Test
    void eliminar_exitoso_devuelve204() throws Exception {
        UUID uuidCarta = UUID.randomUUID();

        mockMvc.perform(delete("/api/cartas-aprobacion/{uuidCarta}", uuidCarta))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_noExiste_devuelve404() throws Exception {
        UUID uuidCarta = UUID.randomUUID();
        doThrow(new RecursoNoEncontradoException("Carta de aprobación no encontrada"))
                .when(cartaAprobacionService).eliminar(uuidCarta);

        mockMvc.perform(delete("/api/cartas-aprobacion/{uuidCarta}", uuidCarta))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_sinBody_devuelve400() throws Exception {
        mockMvc.perform(post("/api/cartas-aprobacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
