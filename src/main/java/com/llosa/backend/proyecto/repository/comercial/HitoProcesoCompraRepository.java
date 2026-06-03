package com.llosa.backend.proyecto.repository.comercial;

import com.llosa.backend.proyecto.entity.comercial.HitoProcesoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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
}
