package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, UUID> {

    @Query("SELECT r FROM Reporte r WHERE r.proyecto.id = :proyecto ORDER BY r.fecha DESC")
    Page<Reporte> findByProyectoId(@Param("proyecto") UUID proyecto, Pageable pageable);
}
