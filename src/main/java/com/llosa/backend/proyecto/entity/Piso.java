package com.llosa.backend.proyecto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "piso")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"torre", "activos", "hitosPiso"})
public class Piso {

    // ATRIBUTOS DE LA CLASE

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_piso")
    private Long id;

    @Column(name = "nro_piso", nullable = false)
    private Integer nroPiso; // Puede ser negativo

    // RELACIONES A LAS DIFERENTES TABLAS

    @OneToMany(mappedBy = "piso",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = true)
    @Builder.Default
    private List<Activo> activos = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_torre", nullable = false)
    private Torre torre;

    @OneToMany(mappedBy = "piso",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = true)
    @Builder.Default
    private List<HitoPiso> hitosPiso = new ArrayList<>();
}
