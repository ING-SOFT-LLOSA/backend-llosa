package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Torre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TorreRepository extends JpaRepository<Torre, Long> {
}
