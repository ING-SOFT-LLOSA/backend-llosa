package com.llosa.backend.comercial.repository;

import com.llosa.backend.comercial.entity.RequisitoDocumental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequisitoDocumentalRepository extends JpaRepository<RequisitoDocumental, UUID> {

    List<RequisitoDocumental> findByHitoComercial_UuidHitoComercialOrderByFechaEmisionDesc(UUID uuidHitoComercial);

    List<RequisitoDocumental> findByHitoComercial_UuidHitoComercialInOrderByFechaEmisionDesc(List<UUID> uuidHitos);
}
