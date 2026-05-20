package com.llosa.backend.proyecto.entity;

import com.llosa.backend.proyecto.enums.TipoActivo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "activo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "piso")
public class Activo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_activo", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_piso", nullable = false)
    private Piso piso;

    @Column(nullable = false)
    private String nro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoActivo tipo;

    @Column(precision = 19, scale = 2)
    private BigDecimal precio;

    private String descripcion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
