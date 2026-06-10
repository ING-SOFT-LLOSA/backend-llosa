package com.llosa.backend.pagos.entity;

import com.llosa.backend.proyecto.entity.UsuarioActivo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cronograma_pago")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"usuarioActivo"})
public class CronogramaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_cronograma", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_usuario_activo", nullable = false, unique = true)
    private UsuarioActivo usuarioActivo;

    @Column(name = "total_pactado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPactado;

    @Builder.Default
    @Column(name = "cuota_inicial", precision = 12, scale = 2)
    private BigDecimal cuotaInicial = BigDecimal.ZERO;

    @Column(name = "numero_cuotas")
    private Integer numeroCuotas;

    @Column(nullable = false, length = 20)
    private String estado;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
