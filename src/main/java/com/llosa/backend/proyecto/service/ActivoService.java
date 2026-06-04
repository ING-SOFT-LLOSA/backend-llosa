package com.llosa.backend.proyecto.service;

import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ActivoService {
    Activo saveFisico(Long pisoId, Activo activo);
    Activo saveIndividual(Long pisoId, Activo activo);
    Activo save(Activo activo);
    Activo findById(UUID id);
    void deleteById(UUID id);
    List<Activo> findByPiso(Long pisoId, String search);
    Page<ActivoResponseDTO> listarPorProyectoYEstado(UUID idProyecto, EstadoComercialActivo estado, int page, int size);
}
