package com.llosa.backend.agenda.repository;

import com.llosa.backend.agenda.entity.Cita;
import com.llosa.backend.agenda.enums.EstadoCita;
import com.llosa.backend.agenda.enums.EstadoSincronizacion;
import com.llosa.backend.agenda.enums.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CitaRepository extends JpaRepository<Cita, UUID> {

    /** Todas las citas de un cliente (para el Portal Cliente). */
    @Query("SELECT c FROM Cita c WHERE c.cliente.id = :clienteId ORDER BY c.fechaInicio ASC")
    List<Cita> findByClienteId(@Param("clienteId") Integer clienteId);

    /** Citas de un cliente filtradas por estado (ej: solo PROGRAMADA o CONFIRMADA). */
    @Query("SELECT c FROM Cita c WHERE c.cliente.id = :clienteId AND c.estadoCita = :estado ORDER BY c.fechaInicio ASC")
    List<Cita> findByClienteIdAndEstado(@Param("clienteId") Integer clienteId,
                                        @Param("estado") EstadoCita estado);

    /** Próximas citas de un cliente (futuras, activas). */
    @Query("""
    SELECT c FROM Cita c
    WHERE c.cliente.id = :clienteId
      AND c.fechaInicio >= :ahora
      AND c.estadoCita IN ('PROGRAMADA', 'CONFIRMADA', 'REPROGRAMACION_PENDIENTE')
    ORDER BY c.fechaInicio ASC
""")
    List<Cita> findProximasByClienteId(@Param("clienteId") Integer clienteId,
                                       @Param("ahora") LocalDateTime ahora);

    /** Citas de un gestor en un rango de fechas (para el calendario del backoffice). */
    @Query("""
        SELECT c FROM Cita c
        WHERE c.gestor.id = :gestorId
          AND c.fechaInicio BETWEEN :inicio AND :fin
        ORDER BY c.fechaInicio ASC
    """)
    List<Cita> findByGestorAndRango(@Param("gestorId") Integer gestorId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fin") LocalDateTime fin);

    /** Todas las citas de una unidad inmobiliaria. */
    @Query("SELECT c FROM Cita c WHERE c.activo.id = :activoId ORDER BY c.fechaInicio DESC")
    List<Cita> findByActivoId(@Param("activoId") UUID activoId);

    /** Citas pendientes de sincronizar con Google Calendar (para el worker de reintentos). */
    List<Cita> findByEstadoSincronizacionAndClienteUsaGoogle(
            EstadoSincronizacion estado, Boolean clienteUsaGoogle);

    /** Citas de recordatorio de pago pendientes de sincronizar. */
    List<Cita> findByEstadoSincronizacionAndTipoEvento(
            EstadoSincronizacion estado, TipoEvento tipoEvento);

    /** Verifica si ya existe una cita en ese rango para el gestor (evita solapamientos). */
    @Query("""
        SELECT COUNT(c) > 0 FROM Cita c
        WHERE c.gestor.id = :gestorId
          AND c.estadoCita NOT IN ('CANCELADA', 'COMPLETADA')
          AND c.fechaInicio < :fin
          AND c.fechaFin > :inicio
    """)
    boolean existeSolapamiento(@Param("gestorId") Integer gestorId,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fin") LocalDateTime fin);

    /** Busca por googleEventId para actualizaciones de sincronización. */
    Optional<Cita> findByGoogleEventId(String googleEventId);

    /** Citas de TODOS los clientes para el backoffice (paginado con búsqueda). */
    @Query("""
        SELECT c FROM Cita c
        WHERE (:activoId IS NULL OR c.activo.id = :activoId)
        ORDER BY c.fechaInicio DESC
    """)
    List<Cita> findAllByActivoOptional(@Param("activoId") UUID activoId);
}
