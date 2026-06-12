package com.llosa.backend.pagos.repository;

import com.llosa.backend.pagos.entity.CronogramaPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CronogramaPagoRepository extends JpaRepository<CronogramaPago, UUID> {

    Optional<CronogramaPago> findByUsuarioActivo_UuidUsuarioActivo(UUID uuidUsuarioActivo);

    boolean existsByUsuarioActivo_UuidUsuarioActivo(UUID uuidUsuarioActivo);
}
