package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Piso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PisoRepository extends JpaRepository<Piso, Long> {

    List<Piso> findByTorre_Id(Long torreId);

    boolean existsByTorre_IdAndNroPiso(Long torreId, Integer nroPiso);
}
