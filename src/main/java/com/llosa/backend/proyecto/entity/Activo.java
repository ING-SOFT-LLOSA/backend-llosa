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
@ToString(exclude = {"piso","hitosUnidad"})
public class Activo {

    // ATRIBUTOS DE LA CLASE

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_activo", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String nro; // Ejemplo : DEPARTAMENTO 408

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoActivo tipo; // Para saber si es area comun o candidato a venta

    @Column(nullable = false, name = "area_m2")
    private BigDecimal areaM2 = BigDecimal.valueOf(0.0);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoComercialActivo estadoComercial;

    private BigDecimal precio = BigDecimal.valueOf(0.0);

    private String descripcion = "No existe descripcion todavia";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // RELACIONES A LAS DIFERENTES TABLAS

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_piso", nullable = false)
    private Piso piso;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HitoUnidad> hitosUnidad = new ArrayList<>();
}
