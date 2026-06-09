package com.llosa.backend.proyecto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reporte")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"proyecto"})
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_reporte", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_proyecto", nullable = false)
    private Proyecto proyecto;

    @Column(name = "titulo_periodo", nullable = false)
    private String tituloPeriodo;

    @Column(name = "porcentaje_avance", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeAvance;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @ElementCollection
    @CollectionTable(
            name = "reporte_hitos",
            joinColumns = @JoinColumn(name = "uuid_reporte")
    )
    @Column(name = "hito")
    @Builder.Default
    private List<String> hitosConsolidados = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
