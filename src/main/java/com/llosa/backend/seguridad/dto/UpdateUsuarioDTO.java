package com.llosa.backend.seguridad.dto;

import lombok.Data;

@Data
public class UpdateUsuarioDTO {
    private String nombre;
    private String apellidos;
    private String email;
    private String telefono;
    private String documentoIdentidad;
    private String tipoUsuario;
}
