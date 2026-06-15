package com.llosa.backend.proyecto.entity;

import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "activo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"piso"})
public class Activo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_activo", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String nro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoActivo tipo;

    // FIX: Agregar @Builder.Default porque tiene valor inicial
    @Builder.Default
    @Column(nullable = false, name = "area_m2")
    private BigDecimal areaM2 = BigDecimal.valueOf(0.0);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoComercialActivo estadoComercial;

    // FIX: Agregar @Builder.Default porque tiene valor inicial
    @Builder.Default
    private BigDecimal precio = BigDecimal.valueOf(0.0);

    // FIX: Agregar @Builder.Default porque tiene valor inicial
    @Builder.Default
    private String descripcion = "No existe descripcion todavia";

    @Builder.Default
    private String linkRecorridoVirtual = "";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_piso", nullable = false)
    private Piso piso;

    // =========================================================================
    // NUEVA RELACIÓN: El contrato al que pertenece actualmente este activo
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_usuario_activo", unique = true) // Es nullable porque en inventario no tiene contrato
    private UsuarioActivo usuarioActivo;
}