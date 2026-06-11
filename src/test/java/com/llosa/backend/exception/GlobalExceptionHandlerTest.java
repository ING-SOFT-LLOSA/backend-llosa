package com.llosa.backend.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEmailDuplicado_devuelveMensajeConflicto() {
        EmailDuplicadoException ex = new EmailDuplicadoException("test@mail.com");

        Map<String, String> result = handler.handleEmailDuplicado(ex);

        assertThat(result).containsKey("error");
        assertThat(result.get("error")).contains("test@mail.com");
    }

    @Test
    void handleNoEncontrado_devuelveMensaje404() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("Recurso no encontrado");

        Map<String, String> result = handler.handleNoEncontrado(ex);

        assertThat(result).containsEntry("error", "Recurso no encontrado");
    }

    @Test
    void handleAccesoDenegado_devuelveMensaje403() {
        AccesoDenegadoException ex = new AccesoDenegadoException("Acceso denegado");

        Map<String, String> result = handler.handleAccesoDenegado(ex);

        assertThat(result).containsEntry("error", "Acceso denegado");
    }

    @Test
    void emailDuplicadoException_mensajePropagado() {
        EmailDuplicadoException ex = new EmailDuplicadoException("test@mail.com");
        assertThat(ex.getMessage()).contains("test@mail.com");
    }

    @Test
    void recursoNoEncontradoException_mensajePropagado() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("no existe");
        assertThat(ex.getMessage()).isEqualTo("no existe");
    }

    @Test
    void accesoDenegadoException_mensajePropagado() {
        AccesoDenegadoException ex = new AccesoDenegadoException("sin acceso");
        assertThat(ex.getMessage()).isEqualTo("sin acceso");
    }

    @Test
    void businessException_mensajePropagado() {
        BusinessException ex = new BusinessException("regla de negocio violada");
        assertThat(ex.getMessage()).isEqualTo("regla de negocio violada");
    }

    @Test
    void apiException_guardaStatusYMensaje() {
        ApiException ex = new ApiException("No autorizado", HttpStatus.UNAUTHORIZED);
        assertThat(ex.getMessage()).isEqualTo("No autorizado");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apiException_esRuntimeException() {
        ApiException ex = new ApiException("error", HttpStatus.BAD_REQUEST);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
