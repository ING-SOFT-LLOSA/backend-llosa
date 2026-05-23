package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.request.ProyectoCargaDTO;
import com.llosa.backend.proyecto.dto.request.ProyectoCreateDTO;
import com.llosa.backend.proyecto.entity.Proyecto;

import java.util.List;
import java.util.UUID;

public interface ProyectoService {

    Proyecto findById(UUID id);

    List<Proyecto> findAll();

    double getPorcentajeAvance(UUID id);

    Proyecto save(Proyecto proyecto);

    void cargarProyecto(UUID idProyecto,ProyectoCargaDTO dto);
}
