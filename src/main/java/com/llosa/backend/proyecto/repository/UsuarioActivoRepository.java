package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.UsuarioActivo;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioActivoRepository extends JpaRepository<UsuarioActivo, UUID> {

    /**
     * Busca todos los procesos comerciales donde un usuario específico
     * aparece como copropietario (cliente).
     */
    @Query("SELECT ua FROM UsuarioActivo ua JOIN ua.clientes c WHERE c.id = :usuarioId")
    List<UsuarioActivo> findByClienteId(@Param("usuarioId") Integer usuarioId);

    /**
     * Busca el proceso comercial activo de un activo (inmueble) específico.
     * Un activo solo puede tener UN proceso comercial vigente en esta tabla.
     */
    Optional<UsuarioActivo> findByActivo_Id(UUID uuidActivo);

    void deleteById(@NonNull UUID id);
}
