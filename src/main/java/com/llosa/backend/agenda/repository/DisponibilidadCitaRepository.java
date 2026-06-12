package com.llosa.backend.agenda.repository;

import com.llosa.backend.agenda.entity.DisponibilidadCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisponibilidadCitaRepository extends JpaRepository<DisponibilidadCita, Long> {

    List<DisponibilidadCita> findByCita_Id(UUID citaId);

    @Modifying
    @Query("UPDATE DisponibilidadCita d SET d.seleccionado = false WHERE d.cita.id = :citaId")
    void deseleccionarTodos(@Param("citaId") UUID citaId);
}
