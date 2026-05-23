package com.llosa.backend.proyecto.repository;


import com.llosa.backend.proyecto.entity.Hito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface HitoRepository extends JpaRepository<Hito, UUID> {
}
