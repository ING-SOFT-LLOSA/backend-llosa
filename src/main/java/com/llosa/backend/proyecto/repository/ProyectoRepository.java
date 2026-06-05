package com.llosa.backend.proyecto.repository;


import com.llosa.backend.proyecto.entity.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProyectoRepository extends JpaRepository<Proyecto, UUID> {
    List<Proyecto> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(String nombre, String descripcion);
    boolean existsByNombreIgnoreCase(String nombre);
}
