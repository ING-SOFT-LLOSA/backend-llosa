package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ActivoRepository extends JpaRepository<Activo, UUID> {
    List<Activo> findByPisoId(Long id);
    List<Activo> findByPisoIdAndNroContainingIgnoreCase(Long pisoId, String nro);

    Page<Activo> findByPisoTorreProyectoId(
            UUID uuidProyecto,
            Pageable pageable
    );
    Page<Activo> findByPisoTorreProyectoIdAndEstadoComercial(
            UUID pisoTorreProyectoId, EstadoComercialActivo estadoComercial, Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Activo a WHERE a.id IN :ids")
    List<Activo> findByIdsForUpdate(@Param("ids") List<UUID> ids);

    Activo findByNro(String nro);
}
