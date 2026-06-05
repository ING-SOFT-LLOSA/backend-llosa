package com.llosa.backend.seguridad.dto;

import lombok.Data;
import java.util.List;

@Data
public class PerfilConPermisosResponse {
    private Integer id;
    private String nombre;
    private String apellidos;
    private String email;
    private String telefono;
    private String tipoUsuario;
    private String rol;
    private Boolean activo;
    private List<String> funciones;
}