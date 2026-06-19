package com.llosa.backend.pagos.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.llosa.backend.config.TestDataPagos;
import com.llosa.backend.exception.*;
import com.llosa.backend.pagos.dto.PagoRequest;
import com.llosa.backend.pagos.dto.PagoResponse;
import com.llosa.backend.pagos.service.PagoService;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    private MockMvc mockMvc;

    @Mock
    PagoService pagoService;

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    Authentication authentication;

    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PagoController(pagoService, usuarioRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RequestPostProcessor withAuth() {
        return request -> {
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    @Test
    void listarPorCronograma_conPagos_devuelve200() throws Exception {
        UUID uuidCp = UUID.randomUUID();
        var pagos = List.of(
                new PagoResponse(UUID.randomUUID(), uuidCp, 1, new BigDecimal("25000.00"),
                        LocalDate.now(), "PENDIENTE", BigDecimal.ZERO, null, null, null, null, null, null, null, null, null),
                new PagoResponse(UUID.randomUUID(), uuidCp, 2, new BigDecimal("25000.00"),
                        LocalDate.now(), "PAGADO", new BigDecimal("25000.00"), LocalDateTime.now(), null, null, null, null, null, null, null, null));

        when(pagoService.listarPorCronograma(uuidCp)).thenReturn(pagos);

        mockMvc.perform(get("/api/cronogramas/{uuidCronograma}/pagos", uuidCp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nroCuota").value(1))
                .andExpect(jsonPath("$[1].estado").value("PAGADO"));
    }

    @Test
    void agregarCuota_conDatosValidos_devuelve201() throws Exception {
        UUID uuidCp = UUID.randomUUID();
        var request = new PagoRequest(1, new BigDecimal("25000.00"), LocalDate.now().plusMonths(1), null, null);
        var response = new PagoResponse(UUID.randomUUID(), uuidCp, 1, request.montoProgramado(),
                request.fechaVencimiento(), "PENDIENTE", BigDecimal.ZERO, null, null, null, null, null, null, null, null, null);

        when(pagoService.agregarCuota(eq(uuidCp), any(PagoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/cronogramas/{uuidCronograma}/pagos", uuidCp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nroCuota").value(1))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void agregarCuota_cuotaDuplicada_devuelve409() throws Exception {
        UUID uuidCp = UUID.randomUUID();
        when(pagoService.agregarCuota(eq(uuidCp), any(PagoRequest.class)))
                .thenThrow(new EntidadDuplicadaException("Ya existe una cuota con el número 1"));

        mockMvc.perform(post("/api/cronogramas/{uuidCronograma}/pagos", uuidCp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataPagos.crearPagoRequest(1))))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizarCuota_exitoso_devuelve200() throws Exception {
        UUID uuidPago = UUID.randomUUID();
        var request = new PagoRequest(2, new BigDecimal("30000.00"), LocalDate.now().plusMonths(2), null, null);
        var response = new PagoResponse(uuidPago, UUID.randomUUID(), 2, request.montoProgramado(),
                request.fechaVencimiento(), "PENDIENTE", BigDecimal.ZERO, null, null, null, null, null, null, null, null, null);

        when(pagoService.actualizarCuota(eq(uuidPago), any(PagoRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/pagos/{uuidPago}", uuidPago)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nroCuota").value(2));
    }

    @Test
    void eliminarCuota_exitoso_devuelve204() throws Exception {
        UUID uuidPago = UUID.randomUUID();

        mockMvc.perform(delete("/api/pagos/{uuidPago}", uuidPago))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarCuota_noExiste_devuelve404() throws Exception {
        UUID uuidPago = UUID.randomUUID();
        doThrow(new RecursoNoEncontradoException("Pago no encontrado"))
                .when(pagoService).eliminarCuota(uuidPago);

        mockMvc.perform(delete("/api/pagos/{uuidPago}", uuidPago))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarEstado_exitoso_devuelve200() throws Exception {
        UUID uuidPago = UUID.randomUUID();
        var usuario = new Usuario();
        usuario.setId(1);

        when(authentication.getPrincipal()).thenReturn("test-uid");
        when(usuarioRepository.findByFirebaseUuid("test-uid")).thenReturn(Optional.of(usuario));
        when(pagoService.cambiarEstado(uuidPago, "PAGADO", 1)).thenReturn(
                new PagoResponse(uuidPago, UUID.randomUUID(), 1, new BigDecimal("25000.00"),
                        LocalDate.now(), "PAGADO", new BigDecimal("25000.00"), LocalDateTime.now(), null, 1, null, null, null, null, null, null));

        mockMvc.perform(patch("/api/pagos/{uuidPago}/estado", uuidPago)
                        .param("estado", "PAGADO")
                        .with(withAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"));
    }

    @Test
    void cambiarEstado_estadoInvalido_devuelve409() throws Exception {
        UUID uuidPago = UUID.randomUUID();
        var usuario = new Usuario();
        usuario.setId(1);

        when(authentication.getPrincipal()).thenReturn("test-uid");
        when(usuarioRepository.findByFirebaseUuid("test-uid")).thenReturn(Optional.of(usuario));
        when(pagoService.cambiarEstado(uuidPago, "INVALIDO", 1))
                .thenThrow(new EstadoInvalidoException("Estado inválido: INVALIDO"));

        mockMvc.perform(patch("/api/pagos/{uuidPago}/estado", uuidPago)
                        .param("estado", "INVALIDO")
                        .with(withAuth()))
                .andExpect(status().isConflict());
    }

    @Test
    void cambiarEstado_pagoNoExiste_devuelve404() throws Exception {
        UUID uuidPago = UUID.randomUUID();
        var usuario = new Usuario();
        usuario.setId(1);

        when(authentication.getPrincipal()).thenReturn("test-uid");
        when(usuarioRepository.findByFirebaseUuid("test-uid")).thenReturn(Optional.of(usuario));
        when(pagoService.cambiarEstado(uuidPago, "PAGADO", 1))
                .thenThrow(new RecursoNoEncontradoException("Pago no encontrado"));

        mockMvc.perform(patch("/api/pagos/{uuidPago}/estado", uuidPago)
                        .param("estado", "PAGADO")
                        .with(withAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void subirComprobante_exitoso_devuelve200() throws Exception {
        UUID uuidPago = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        var usuario = new Usuario();
        usuario.setId(1);

        var response = new PagoResponse(uuidPago, UUID.randomUUID(), 1, new BigDecimal("25000.00"),
                LocalDate.now(), "PAGADO", new BigDecimal("25000.00"), LocalDateTime.now(),
                docId, 1, null, null, null, null, null, null);

        MockMultipartFile file = new MockMultipartFile("file", "comprobante.pdf",
                "application/pdf", "contenido".getBytes());

        when(authentication.getPrincipal()).thenReturn("test-uid");
        when(usuarioRepository.findByFirebaseUuid("test-uid")).thenReturn(Optional.of(usuario));
        when(pagoService.subirComprobante(eq(uuidPago), any(), eq(1), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/pagos/{uuidPago}/comprobante", uuidPago)
                        .file(file)
                        .with(withAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuidComprobante").value(docId.toString()))
                .andExpect(jsonPath("$.estado").value("PAGADO"));
    }

    @Test
    void subirComprobante_sinArchivo_devuelve400() throws Exception {
        UUID uuidPago = UUID.randomUUID();

        mockMvc.perform(multipart("/api/pagos/{uuidPago}/comprobante", uuidPago))
                .andExpect(status().isBadRequest());
    }
}
