package com.llosa.backend.proyecto.entity;

import com.llosa.backend.proyecto.enums.EstadoEtapa;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "etapa")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"proyecto", "hitos"})
public class Etapa {
    // Atributos de la clase
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etapa")
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Integer orden;

    @Column(length = 200)
    private String descripcion = "Descripcion aún no definida";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEtapa estado;

    // RELACIONES A LAS DIFERENTES TABLAS
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_proyecto", nullable = false)
    private Proyecto proyecto;

    @OneToMany(mappedBy = "etapa",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               orphanRemoval = true)
    @Builder.Default
    private List<Hito> hitos = new ArrayList<>();
}
