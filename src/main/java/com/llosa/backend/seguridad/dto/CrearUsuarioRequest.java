package com.llosa.backend.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CrearUsuarioRequest {
    @NotBlank private String nombre;
    @NotBlank private String apellidos;
    @NotBlank @Email private String email;
    private String telefono;
    private String documentoIdentidad;
    @NotBlank private String tipoUsuario;
    private Integer idRol;
}