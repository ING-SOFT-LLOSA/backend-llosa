package com.llosa.backend.proyecto.repository;


import com.llosa.backend.proyecto.entity.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProyectoRepository extends JpaRepository<Proyecto, UUID> {
}
