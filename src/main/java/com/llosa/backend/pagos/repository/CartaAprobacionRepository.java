package com.llosa.backend.pagos.repository;

import com.llosa.backend.pagos.entity.CartaAprobacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartaAprobacionRepository extends JpaRepository<CartaAprobacion, UUID> {

    Optional<CartaAprobacion> findByUsuarioActivo_UuidUsuarioActivo(UUID uuidUsuarioActivo);

    boolean existsByUsuarioActivo_UuidUsuarioActivo(UUID uuidUsuarioActivo);
}
