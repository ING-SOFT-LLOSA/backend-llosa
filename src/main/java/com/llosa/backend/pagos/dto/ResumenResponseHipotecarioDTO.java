package com.llosa.backend.pagos.dto;

import com.llosa.backend.pagos.EstadoGlobalPago;

import java.math.BigDecimal;

public record ResumenResponseHipotecarioDTO(
        BigDecimal montoTotal,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente,
        EstadoGlobalPago estadoGlobal
) {
}
