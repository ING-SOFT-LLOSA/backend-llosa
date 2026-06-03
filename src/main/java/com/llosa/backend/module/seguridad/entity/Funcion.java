package com.llosa.backend.module.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "funcion")
@Getter @Setter
public class Funcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_funcion")
    private Integer idFuncion;

    @Column(name = "nombre_codigo", nullable = false, unique = true, length = 50)
    private String nombreCodigo;

    @Column(length = 200)
    private String descripcion;
}