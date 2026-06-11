package com.llosa.backend.comercial.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StageActivosResponse(
        String resumen,
        Integer totalActivos,
        List<ActivoItemResponse> activos
) {
    public record ActivoItemResponse(
            UUID uuidActivo,
            String nro,
            String tipo,
            BigDecimal areaM2,
            BigDecimal precio,
            String descripcion,
            String linkRecorridoVirtual
    ) {}
}