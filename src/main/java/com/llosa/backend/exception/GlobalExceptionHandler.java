package com.llosa.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // Convierte el error en un HTTP 404
    public Map<String, String> handleEntityNotFound(EntityNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(EmailDuplicadoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleEmailDuplicado(EmailDuplicadoException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAccesoDenegado(AccesoDenegadoException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // <-- Cambiado de UNPROCESSABLE_ENTITY (422) a BAD_REQUEST (400)
    public Map<String, String> handleBusinessException(BusinessException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // <-- Esto es el código 409
    public Map<String, String> handleEstadoInvalido(EstadoInvalidoException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(EntidadDuplicadaException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // <-- Esto genera el HTTP 409
    public Map<String, String> handleEntidadDuplicada(EntidadDuplicadaException ex) {
        return Map.of("error", ex.getMessage());
    }
}
