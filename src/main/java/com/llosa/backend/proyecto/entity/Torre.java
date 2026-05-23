package com.llosa.backend.proyecto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "torre")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"proyecto", "pisos"})
public class Torre {

    // Atributos de la clase

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_torre")
    private Long id;

    @Column(nullable = false)
    private String nombre;

    // RELACIONES A LAS DIFERENTES TABLAS

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_proyecto", nullable = false)
    private Proyecto proyecto;

    @OneToMany(mappedBy = "torre",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @Builder.Default
    private List<Piso> pisos = new ArrayList<>();


}
