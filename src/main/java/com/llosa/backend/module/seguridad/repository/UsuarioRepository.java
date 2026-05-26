package com.llosa.backend.module.seguridad.repository;

import com.llosa.backend.module.seguridad.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByFirebaseUuid(String firebaseUuid);
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}