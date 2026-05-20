package com.llosa.backend.proyecto.repository;


import com.llosa.backend.proyecto.entity.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoRepository extends JpaRepository<Proyecto, UUID> {

    List<Proyecto> findByDistrito(String distrito);

    List<Proyecto> findByNombreContainingIgnoreCase(String nombre);

    @Query("""
            SELECT DISTINCT p FROM Proyecto p
            LEFT JOIN FETCH p.torres t
            LEFT JOIN FETCH t.pisos
            WHERE p.id = :id
            """)
    Optional<Proyecto> findByIdWithTorresAndPisos(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT p FROM Proyecto p
            LEFT JOIN FETCH p.torres t
            LEFT JOIN FETCH t.pisos pi
            LEFT JOIN FETCH pi.activos
            WHERE p.id = :id
            """)
    Optional<Proyecto> findByIdWithArbolFisico(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT p FROM Proyecto p
            LEFT JOIN FETCH p.etapas e
            LEFT JOIN FETCH e.hitos
            WHERE p.id = :id
            """)
    Optional<Proyecto> findByIdWithCronograma(@Param("id") UUID id);
}
