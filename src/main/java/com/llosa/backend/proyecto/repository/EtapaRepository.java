package com.llosa.backend.proyecto.repository;


import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.entity.Proyecto;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtapaRepository extends JpaRepository<Etapa, Long> {
    Proyecto findProyectoById(Long id);

    void deleteById(@NonNull Long id);
}
