package com.llosa.backend.module.seguridad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AsignarRolRequest {
    @NotNull private Integer idRol;
}