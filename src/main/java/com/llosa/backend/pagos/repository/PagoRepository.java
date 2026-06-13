package com.llosa.backend.pagos.repository;

import com.llosa.backend.pagos.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagoRepository extends JpaRepository<Pago, UUID> {

    List<Pago> findByCronograma_IdOrderByNroCuotaAsc(UUID cronogramaId);

    Optional<Pago> findByCronograma_IdAndNroCuota(UUID cronogramaId, Integer nroCuota);

    @Query("SELECT COALESCE(SUM(p.montoPagado), 0) FROM Pago p WHERE p.cronograma.id = :cronogramaId")
    BigDecimal sumMontoPagadoByCronogramaId(@Param("cronogramaId") UUID cronogramaId);

    long countByCronograma_IdAndEstado(UUID cronogramaId, String estado);
}
