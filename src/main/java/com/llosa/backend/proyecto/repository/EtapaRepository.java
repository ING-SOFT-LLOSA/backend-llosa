package com.llosa.backend.proyecto.repository;


import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.enums.EstadoEtapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EtapaRepository extends JpaRepository<Etapa, Long> {

    List<Etapa> findByProyecto_Id(UUID proyectoId);

    List<Etapa> findByProyecto_IdAndEstado(UUID proyectoId, EstadoEtapa estado);

    @Query("""
            SELECT e FROM Etapa e
            LEFT JOIN FETCH e.hitos
            WHERE e.id = :id
            """)
    Optional<Etapa> findByIdWithHitos(@Param("id") Long id);
}
