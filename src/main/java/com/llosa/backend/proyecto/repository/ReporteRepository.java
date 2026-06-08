package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, UUID> {

    Page<Reporte> findByProyectoId(UUID proyectoId, Pageable pageable);
}
