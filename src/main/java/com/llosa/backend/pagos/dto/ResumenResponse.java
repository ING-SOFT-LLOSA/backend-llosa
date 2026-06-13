package com.llosa.backend.pagos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenResponse(
        BigDecimal totalPactado,
        BigDecimal totalPagado,
        BigDecimal totalPendiente,
        String estadoGlobal,
        long cuotasPagadas,
        long cuotasPendientes,
        long cuotasVencidas,
        LocalDate proximoVencimiento
) {}
