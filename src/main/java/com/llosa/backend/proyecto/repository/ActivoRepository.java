package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ActivoRepository extends JpaRepository<Activo, UUID> {
    List<Activo> findByPisoId(Long id);
    List<Activo> findByPisoIdAndNroContainingIgnoreCase(Long pisoId, String nro);
    List<Activo> findByPisoTorreProyectoId(UUID id);
    Page<Activo> findByPisoTorreProyectoId(
            UUID uuidProyecto,
            Pageable pageable
    );
    Page<Activo> findByPisoTorreProyectoIdAndEstadoComercial(
            UUID piso_torre_proyecto_id, EstadoComercialActivo estadoComercial, Pageable pageable
    );
}
