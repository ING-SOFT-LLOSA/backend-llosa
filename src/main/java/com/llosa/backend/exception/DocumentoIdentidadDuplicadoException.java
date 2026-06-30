package com.llosa.backend.exception;

public class DocumentoIdentidadDuplicadoException extends RuntimeException {

    public DocumentoIdentidadDuplicadoException(String documentoIdentidad) {
        super("Ya existe un usuario registrado con el documento de identidad: " + documentoIdentidad);
    }
}