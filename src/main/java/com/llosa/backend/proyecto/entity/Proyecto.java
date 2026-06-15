package com.llosa.backend.proyecto.entity;

import com.llosa.backend.documentos.entity.Documento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Entity
@Table(name = "proyecto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"torres", "hitos"})
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_proyecto", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String descripcion;

    // FIX: Added @Builder.Default
    @Builder.Default
    private Boolean precertificacionEdgeLeed = false;

    private String departamento;

    private String distrito;

    private String direccion;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "proyecto",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @Builder.Default
    private List<Torre> torres = new ArrayList<>();

    @OneToMany(mappedBy = "proyecto",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @Builder.Default
    private List<Hito> hitos = new ArrayList<>();
}