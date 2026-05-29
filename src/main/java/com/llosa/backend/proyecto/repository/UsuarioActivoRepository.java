package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.UsuarioActivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioActivoRepository extends JpaRepository<UsuarioActivo, Long> {
    List<UsuarioActivo> findByUsuario_Id(Integer usuarioId);
    List<UsuarioActivo> findByActivo_Id(UUID activoId);
    List<UsuarioActivo> findByUsuarioEmail(String email);

    Optional<UsuarioActivo> findByActivoId(UUID uuidActivo);
}
