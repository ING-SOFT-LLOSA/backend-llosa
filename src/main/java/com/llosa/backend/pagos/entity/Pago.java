package com.llosa.backend.pagos.entity;

import com.llosa.backend.pagos.ConceptoPago;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pago", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"uuid_cronograma", "nro_cuota"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"cronograma"})
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_pago", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_cronograma", nullable = false)
    private CronogramaPago cronograma;

    @Column(name = "nro_cuota", nullable = false)
    private Integer nroCuota;

    @Column(name = "monto_programado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoProgramado;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false, length = 20)
    private String estado;

    @Builder.Default
    @Column(name = "monto_pagado", precision = 12, scale = 2)
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "uuid_comprobante")
    private UUID uuidComprobante;

    @Column(name = "actualizado_por")
    private Integer actualizadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "concepto", nullable = false, length = 30)
    private ConceptoPago concepto;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "uuid_requisito_documental")
    private UUID uuidRequisitoDocumental;

    @Column(name = "uuid_cita")
    private UUID uuidCita;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
