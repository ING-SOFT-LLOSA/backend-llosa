package com.llosa.backend.proyecto.dto.request;

import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ActivoRequestDTO(

        @NotBlank(message = "El número del activo es obligatorio")
        String nro,

        @NotNull(message = "El tipo de activo es obligatorio")
        TipoActivo tipo,

        @NotNull(message = "El área es obligatoria")
        @Positive(message = "El área debe ser mayor a 0")
        BigDecimal areaM2,

        @NotNull(message = "El área es obligatoria")
        @PositiveOrZero(message = "El área no puede ser negativa")
        BigDecimal areaTechada,

        @NotNull(message = "El estado comercial es obligatorio")
        EstadoComercialActivo estadoComercial,

        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        BigDecimal precio,

        String descripcion,

        Boolean tieneRecorridoVirtual
) {}