package com.llosa.backend.seguridad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ModificarFuncionesRequest {
    @NotNull private List<Integer> idFunciones;
}