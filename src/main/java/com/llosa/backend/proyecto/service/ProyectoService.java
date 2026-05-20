package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.entity.Proyecto;

import java.util.List;
import java.util.UUID;

public interface ProyectoService {

    Proyecto save(Proyecto proyecto);

    Proyecto findById(UUID id);

    Proyecto findByIdWithTorresAndPisos(UUID id);

    Proyecto findArbolFisico(UUID id);

    Proyecto findCronograma(UUID id);

    List<Proyecto> findAll();

    List<Proyecto> findByDistrito(String distrito);

    List<Proyecto> findByNombre(String nombre);

    Proyecto update(UUID id, Proyecto datos);

    void delete(UUID id);
}
