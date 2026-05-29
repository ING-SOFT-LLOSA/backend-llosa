package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.HitoUnidad;
import com.llosa.backend.proyecto.enums.EstadoHito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HitoUnidadRepository extends JpaRepository<HitoUnidad, UUID> {

    Optional<HitoUnidad> findByActivoAndHito_Orden(Activo activo, Integer orden);

    @Query("SELECT COUNT(hu) FROM HitoUnidad hu WHERE hu.activo.piso.torre.proyecto.id = :proyectoId AND hu.estado = :estado")
    long countByProyectoIdAndEstado(@Param("proyectoId") UUID proyectoId, @Param("estado") EstadoHito estado);

    @Query("SELECT COUNT(hu) FROM HitoUnidad hu WHERE hu.activo.piso.torre.proyecto.id = :proyectoId")
    long countByProyectoId(@Param("proyectoId") UUID proyectoId);

    List<HitoUnidad> findByActivo_IdOrderByHito_OrdenAsc(UUID activoId);

}
