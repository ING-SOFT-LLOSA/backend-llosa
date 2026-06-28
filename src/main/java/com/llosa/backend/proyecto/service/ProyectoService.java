package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.request.ProyectoCargaDTO;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.Proyecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProyectoService {

    Proyecto findById(UUID id);

    Page<Proyecto> findAll(String search, Pageable pageable);

    double getPorcentajeAvance(UUID id);

    Proyecto save(Proyecto proyecto);

    void cargarProyecto(UUID idProyecto,ProyectoCargaDTO dto);

    void deleteById(UUID id);

    List<Hito> findHitosByProyecto(UUID idProyecto);

}
