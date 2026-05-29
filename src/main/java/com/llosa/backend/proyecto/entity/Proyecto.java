package com.llosa.backend.proyecto.entity;

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
@ToString(exclude = {"torres", "etapas"})
public class Proyecto {
    // Atributos de la relación en la tabla
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_proyecto", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    private Boolean precertificacionEdgeLeed = false;

    private String linkRecorridoVirtual = ""; // cambiar esto a activo

    private String departamento;

    private String distrito;

    private String direccion;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    // RELACIONES A LAS DIFERENTES TABLAS
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
    private List<Etapa> etapas = new ArrayList<>();
    // RELACION DE DOCUMENTOS AUN NO REALIZADA
}
