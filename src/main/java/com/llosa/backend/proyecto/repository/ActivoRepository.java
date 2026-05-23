package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Activo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ActivoRepository extends JpaRepository<Activo, UUID> {
}
