package com.llosa.backend.proyecto.repository;


import com.llosa.backend.proyecto.entity.Torre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TorreRepository extends JpaRepository<Torre, Long> {

    List<Torre> findByProyecto_Id(UUID proyectoId);

    @Query("""
            SELECT t FROM Torre t
            LEFT JOIN FETCH t.pisos
            WHERE t.id = :id
            """)
    Optional<Torre> findByIdWithPisos(@Param("id") Long id);
}
