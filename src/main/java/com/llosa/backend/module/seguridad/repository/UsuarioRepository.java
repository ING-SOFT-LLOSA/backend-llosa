package com.llosa.backend.module.seguridad.repository;

import com.llosa.backend.module.seguridad.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByFirebaseUuid(String firebaseUuid);
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("SELECT u FROM Usuario u WHERE " +
            "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "u.documentoIdentidad LIKE CONCAT('%', :search, '%')")
    Page<Usuario> buscarUsuariosPaginados(@Param("search") String search, Pageable pageable);

}