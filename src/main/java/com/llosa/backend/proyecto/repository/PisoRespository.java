package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Piso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PisoRespository extends JpaRepository<Piso, Long> {
}
