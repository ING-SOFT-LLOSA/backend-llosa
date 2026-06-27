package com.llosa.backend.proyecto.repository;


import com.llosa.backend.proyecto.entity.Proyecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProyectoRepository extends JpaRepository<Proyecto, UUID> {
    Page<Proyecto> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(
            String nombre,
            String descripcion,
            Pageable pageable
    );

    Boolean existsByNombre(String nombre);
}
