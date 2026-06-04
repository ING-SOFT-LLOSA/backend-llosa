package com.llosa.backend.module.seguridad.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UsuarioResponse {
    private Integer id;
    private String nombre;
    private String apellidos;
    private String email;
    private String tipoUsuario;
    private String documentoIdentidad;
    private String telefono;
    private String rol;
    private Boolean activo;
    private LocalDateTime createdAt;
    private List<String> funciones;
}