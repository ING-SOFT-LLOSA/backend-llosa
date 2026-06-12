package com.llosa.backend.proyecto.entity;

import com.llosa.backend.proyecto.enums.EstadoHito;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hito")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"proyecto","hitosPiso"})
public class Hito {

    // Atributos de la clase

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_hito", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private Integer orden;

    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoHito estado;

    @Column(name = "fecha_completado")
    private LocalDate fechaCompletado;

    // RELACIONES A LAS DIFERENTES TABLAS

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_proyecto", nullable = false)
    private Proyecto proyecto;

    @OneToMany(mappedBy = "hito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HitoPiso> hitosPiso = new ArrayList<>();
}
