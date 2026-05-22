package com.llosa.backend.exception;

public class EmailDuplicadoException extends RuntimeException {
    public EmailDuplicadoException(String email) {
        super("El correo ya está registrado: " + email);
    }
}
