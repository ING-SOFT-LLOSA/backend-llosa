package com.llosa.backend.comercial.repository;

import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.enums.EtapaProceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EtapaExpedienteRepository extends JpaRepository<EtapaExpediente, UUID> {

    List<EtapaExpediente> findByUsuarioActivo_UuidUsuarioActivo(UUID uuidUsuarioActivo);

    List<EtapaExpediente> findByUsuarioActivo_UuidUsuarioActivoOrderByEtapaProcesoAsc(UUID uuidUsuarioActivo);

    Optional<EtapaExpediente> findByUuidEtapaExpedienteAndUsuarioActivo_UuidUsuarioActivo(
            UUID uuidEtapaExpediente,
            UUID uuidUsuarioActivo
    );

    Optional<EtapaExpediente> findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(
            UUID uuidUsuarioActivo,
            EtapaProceso etapaProceso
    );

    boolean existsByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(
            UUID uuidUsuarioActivo,
            EtapaProceso etapaProceso
    );
}
