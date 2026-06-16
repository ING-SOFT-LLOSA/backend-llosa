package com.llosa.backend.exception;

public class EstadoInvalidoException extends RuntimeException {
  public EstadoInvalidoException(String message) {
    super(message);
  }
}