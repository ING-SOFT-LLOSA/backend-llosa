package com.llosa.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR = "error";

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // Convierte el error en un HTTP 404
    public Map<String, String> handleEntityNotFound(EntityNotFoundException ex) {
        return Map.of(ERROR, ex.getMessage());
    }

    // FIX 0000850: cuando el archivo supera el límite configurado en
    // application.properties, el contenedor/servlet aborta la subida antes de
    // completarla. Sin este handler, el error llegaba como un 500 genérico.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return Map.of(ERROR, "El archivo enviado supera el tamaño máximo permitido por el servidor.");
    }

    @ExceptionHandler(EmailDuplicadoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleEmailDuplicado(EmailDuplicadoException ex) {
        return Map.of(ERROR, ex.getMessage());
    }

    // FIX 0000799: devuelve 409 con mensaje legible cuando se intenta registrar
    // o actualizar un usuario con un documento de identidad ya existente.
    @ExceptionHandler(DocumentoIdentidadDuplicadoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDocumentoIdentidadDuplicado(DocumentoIdentidadDuplicadoException ex) {
        return Map.of(ERROR, ex.getMessage());
    }

    // FIX 0000799: red de seguridad para violaciones de constraint de BD que
    // escapen al chequeo explícito previo (p.ej. condición de carrera entre
    // dos peticiones concurrentes). Evita que lleguen al cliente como HTTP 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String causa = ex.getMostSpecificCause().getMessage();
        if (causa != null && causa.contains("documento_identidad")) {
            return Map.of(ERROR, "Ya existe un usuario registrado con ese documento de identidad.");
        }
        if (causa != null && causa.contains("email")) {
            return Map.of(ERROR, "Ya existe un usuario registrado con ese correo electrónico.");
        }
        return Map.of(ERROR, "No se pudo completar la operación por un conflicto de datos únicos.");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return Map.of(ERROR, ex.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAccesoDenegado(AccesoDenegadoException ex) {
        return Map.of(ERROR, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // <-- Cambiado de UNPROCESSABLE_ENTITY (422) a BAD_REQUEST (400)
    public Map<String, String> handleBusinessException(BusinessException ex) {
        return Map.of(ERROR, ex.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // <-- Esto es el código 409
    public Map<String, String> handleEstadoInvalido(EstadoInvalidoException ex) {
        return Map.of(ERROR, ex.getMessage());
    }

    @ExceptionHandler(EntidadDuplicadaException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // <-- Esto genera el HTTP 409
    public Map<String, String> handleEntidadDuplicada(EntidadDuplicadaException ex) {
        return Map.of(ERROR, ex.getMessage());
    }
}