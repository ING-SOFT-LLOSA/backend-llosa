package com.llosa.backend.proyecto.repository;

import com.llosa.backend.proyecto.entity.Piso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PisoRepository extends JpaRepository<Piso, Long> {
    
    List<Piso> findByTorreId(Long torreId);

    @Query("SELECT p FROM Piso p WHERE p.torre.id = :torreId AND " +
           "CAST(p.nroPiso AS string) LIKE %:search%")
    List<Piso> findByTorreIdAndSearch(@Param("torreId") Long torreId, @Param("search") String search);
}
