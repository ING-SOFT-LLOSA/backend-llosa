package com.llosa.backend.proyecto.entity;

import com.llosa.backend.proyecto.enums.EstadoHito;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hito_unidad", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"uuid_activo", "uuid_hito"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class HitoUnidad {

    // Atributos de la clase

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_hito_unidad", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoHito estado;

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String observaciones;

    // Relaciones con otras tablas

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_activo", nullable = false)
    private Activo activo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_hito", nullable = false)
    private Hito hito;
}
