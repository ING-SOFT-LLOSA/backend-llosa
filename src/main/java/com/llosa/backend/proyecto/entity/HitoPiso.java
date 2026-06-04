package com.llosa.backend.proyecto.entity;

import com.llosa.backend.proyecto.enums.EstadoHito;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hito_piso", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_piso", "uuid_hito"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class HitoPiso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_hito_piso", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoHito estado;

    @Column(name = "fecha_completado")
    private LocalDate fechaCompletado;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_piso", nullable = false)
    private Piso piso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_hito", nullable = false)
    private Hito hito;
}
