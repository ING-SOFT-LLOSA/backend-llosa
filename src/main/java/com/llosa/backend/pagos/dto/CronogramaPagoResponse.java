package com.llosa.backend.pagos.dto;

import com.llosa.backend.pagos.entity.CronogramaPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CronogramaPagoResponse(
        UUID uuidCronograma,
        UUID uuidUsuarioActivo,
        BigDecimal totalPactado,
        BigDecimal cuotaInicial,
        Integer numeroCuotas,
        String estado,
        BigDecimal pagoSeparacion,
        BigDecimal pagoInicial,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CronogramaPagoResponse fromEntity(CronogramaPago cp) {
        return new CronogramaPagoResponse(
                cp.getId(),
                cp.getUsuarioActivo().getUuidUsuarioActivo(),
                cp.getTotalPactado(),
                cp.getCuotaInicial(),
                cp.getNumeroCuotas(),
                cp.getEstado(),
                cp.getPagoSeparacion(),
                cp.getPagoInicial(),
                cp.getCreatedAt(),
                cp.getUpdatedAt()
        );
    }
}
