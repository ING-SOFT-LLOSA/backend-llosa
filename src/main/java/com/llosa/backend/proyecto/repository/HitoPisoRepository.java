package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.enums.EstadoHito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HitoPisoRepository extends JpaRepository<HitoPiso, UUID> {

    @Query("SELECT hp FROM HitoPiso hp JOIN hp.piso p JOIN p.activos a WHERE a.id = :activoId ORDER BY hp.hito.orden ASC")
    List<HitoPiso> findByActivoIdOrderByHitoOrdenAsc(@Param("activoId") UUID activoId);

    java.util.Optional<HitoPiso> findByPisoAndHito_Orden(com.llosa.backend.proyecto.entity.Piso piso, Integer orden);

    @Query("SELECT COUNT(hp) FROM HitoPiso hp WHERE hp.piso.torre.proyecto.id = :proyectoId AND hp.estado = :estado")
    long countByProyectoIdAndEstado(@Param("proyectoId") UUID proyectoId, @Param("estado") EstadoHito estado);

    @Query("SELECT COUNT(hp) FROM HitoPiso hp WHERE hp.piso.torre.proyecto.id = :proyectoId")
    long countByProyectoId(@Param("proyectoId") UUID proyectoId);

    boolean existsByPisoId(Long pisoId);

    boolean existsByPisoIdAndHitoId(Long pisoId, UUID hitoId);

    long countByHitoId(UUID hitoId);

    @Query("SELECT hp FROM HitoPiso hp WHERE hp.piso.torre.id = :torreId AND hp.hito.id = :hitoId")
    List<HitoPiso> findByTorreIdAndHitoId(@Param("torreId") Long torreId, @Param("hitoId") UUID hitoId);
}
