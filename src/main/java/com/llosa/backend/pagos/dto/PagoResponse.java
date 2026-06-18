package com.llosa.backend.pagos.dto;

import com.llosa.backend.pagos.ConceptoPago;
import com.llosa.backend.pagos.entity.Pago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagoResponse(
        UUID uuidPago,
        UUID uuidCronograma,
        Integer nroCuota,
        BigDecimal montoProgramado,
        LocalDate fechaVencimiento,
        String estado,
        BigDecimal montoPagado,
        LocalDateTime fechaPago,
        UUID uuidComprobante,
        Integer actualizadoPor,
        ConceptoPago concepto,
        String comentario,
        UUID uuidRequisitoDocumental,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PagoResponse fromEntity(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getCronograma().getId(),
                pago.getNroCuota(),
                pago.getMontoProgramado(),
                pago.getFechaVencimiento(),
                pago.getEstado(),
                pago.getMontoPagado(),
                pago.getFechaPago(),
                pago.getUuidComprobante(),
                pago.getActualizadoPor(),
                pago.getConcepto(),
                pago.getComentario(),
                pago.getUuidRequisitoDocumental(),
                pago.getCreatedAt(),
                pago.getUpdatedAt()
        );
    }
}
