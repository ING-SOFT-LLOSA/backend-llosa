package com.llosa.backend.proyecto.dto.comercial;

import com.llosa.backend.proyecto.entity.comercial.EtapaProceso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO de entrada para crear o editar un hito del proceso de compra comercial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HitoComercialRequest {

    @NotNull(message = "El UUID del usuario activo es obligatorio")
    private UUID uuidUsuarioActivo;

    @NotNull(message = "La etapa del proceso es obligatoria")
    private EtapaProceso etapaProceso;

    @NotBlank(message = "El nombre del hito es obligatorio")
    private String nombreHito;

    private String descripcion;

    @NotNull(message = "El orden es obligatorio")
    private Integer orden;
}
