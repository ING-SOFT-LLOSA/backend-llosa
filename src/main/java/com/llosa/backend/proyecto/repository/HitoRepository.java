package com.llosa.backend.proyecto.repository;


import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.enums.EstadoHito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HitoRepository extends JpaRepository<Hito, UUID> {

    List<Hito> findByEtapa_Id(Long etapaId);

    List<Hito> findByEtapa_IdAndEstado(Long etapaId, EstadoHito estado);
}
