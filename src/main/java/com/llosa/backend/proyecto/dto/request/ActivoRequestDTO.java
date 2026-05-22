package com.llosa.backend.proyecto.dto.request;

import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;

import java.math.BigDecimal;

public record ActivoRequestDTO(
        String nro,
        TipoActivo tipo,
        BigDecimal areaM2,
        EstadoComercialActivo estadoComercial,
        BigDecimal precio,
        String descripcion
) {
}
