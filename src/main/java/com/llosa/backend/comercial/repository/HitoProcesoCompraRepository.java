package com.llosa.backend.comercial.repository;

import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.enums.EtapaProceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HitoProcesoCompraRepository extends JpaRepository<HitoProcesoCompra, UUID> {

    /**
     * Busca todos los hitos de un UsuarioActivo, ordenados por el campo 'orden' ascendente.
     */
    List<HitoProcesoCompra> findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(UUID uuidUsuarioActivo);

    /**
     * Verifica si existen hitos para un UsuarioActivo dado.
     */
    boolean existsByUsuarioActivo_UuidUsuarioActivo(UUID uuidUsuarioActivo);

    Optional<HitoProcesoCompra> findByUsuarioActivo_UuidUsuarioActivoAndOrden(
            UUID uuidUsuarioActivo,
            Integer orden
    );

    Optional<HitoProcesoCompra> findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(
            UUID uuidUsuarioActivo,
            EtapaProceso etapaProceso
    );
}
