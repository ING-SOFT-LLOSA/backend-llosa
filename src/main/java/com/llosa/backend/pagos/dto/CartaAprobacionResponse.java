package com.llosa.backend.pagos.dto;

import com.llosa.backend.pagos.entity.CartaAprobacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CartaAprobacionResponse(
        UUID uuidCarta,
        UUID uuidUsuarioActivo,
        String banco,
        BigDecimal montoAprobado,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        LocalDate fechaDesembolsoProyectada,
        BigDecimal pagoSeparacion,
        BigDecimal pagoInicial,
        Boolean pagoCompleto,
        String comentarios,
        LocalDateTime createdAt
) {
    public static CartaAprobacionResponse fromEntity(CartaAprobacion ca) {
        return new CartaAprobacionResponse(
                ca.getId(),
                ca.getUsuarioActivo().getUuidUsuarioActivo(),
                ca.getBanco(),
                ca.getMontoAprobado(),
                ca.getFechaEmision(),
                ca.getFechaVencimiento(),
                ca.getFechaDesembolsoProyectada(),
                ca.getPagoSeparacion(),
                ca.getPagoInicial(),
                ca.getPagoCompleto(),
                ca.getComentarios(),
                ca.getCreatedAt()
        );
    }
}
