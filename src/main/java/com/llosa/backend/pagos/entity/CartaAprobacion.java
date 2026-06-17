package com.llosa.backend.pagos.entity;

import com.llosa.backend.proyecto.entity.UsuarioActivo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "carta_aprobacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"usuarioActivo"})
public class CartaAprobacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_carta", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_usuario_activo", nullable = false)
    private UsuarioActivo usuarioActivo;

    @Column(nullable = false, length = 100)
    private String banco;

    @Column(name = "monto_aprobado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoAprobado;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_desembolso_proyectada")
    private LocalDate fechaDesembolsoProyectada;

    // pago de separacion, y cuota inicial
    @Column(name = "pago_separacion", precision = 12, scale = 2)
    private BigDecimal pagoSeparacion;

    @Column(name = "pago_inicial", precision = 12, scale = 2)
    private BigDecimal pagoInicial;

    // booleano de pago completo o pago imcompleto
    @Column(name = "pago_completo")
    private Boolean pagoCompleto;

    @Column(columnDefinition = "TEXT")
    private String comentarios;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
