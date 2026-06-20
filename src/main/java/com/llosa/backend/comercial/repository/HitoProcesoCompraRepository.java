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
    List<HitoProcesoCompra> findByEtapaExpediente_UuidEtapaExpedienteOrderByOrdenAsc(UUID uuidUsuarioActivo);

    /**
     * Verifica si existen hitos para un UsuarioActivo dado.
     */
    Optional<HitoProcesoCompra> findByEtapaExpediente_UuidEtapaExpedienteAndOrden(
            UUID uuidUsuarioActivo,
            Integer orden
    );

    Optional<HitoProcesoCompra> findByEtapaExpediente_UsuarioActivo_UuidUsuarioActivoAndEtapaExpediente_EtapaProcesoOrderByOrdenAsc(
            UUID uuidUsuarioActivo,
            EtapaProceso etapaProceso
    );
}
