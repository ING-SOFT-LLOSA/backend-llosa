package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.enums.TipoActivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ActivoRepository extends JpaRepository<Activo, UUID> {

    List<Activo> findByPiso_Id(Long pisoId);

    List<Activo> findByTipo(TipoActivo tipo);

    @Query("SELECT a FROM Activo a WHERE a.piso.torre.id = :torreId")
    List<Activo> findByTorreId(@Param("torreId") Long torreId);

    @Query("SELECT a FROM Activo a WHERE a.piso.torre.proyecto.id = :proyectoId")
    List<Activo> findByProyectoId(@Param("proyectoId") UUID proyectoId);
}
