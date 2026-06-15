package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.repository.ActivoRepository;

import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.HidratationService;
import com.llosa.backend.proyecto.service.PisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivoServiceImpl implements ActivoService {

    private final ActivoRepository activoRepository;
    private final PisoService pisoService;
    private final HidratationService hidratationService;

    @Override
    @Transactional
    public Activo saveFisico(Long pisoId, Activo activo){
        Piso piso = pisoService.findById(pisoId);
        activo.setPiso(piso);
        return activoRepository.save(activo);
    }

    @Override
    @Transactional
    public Activo saveIndividual(Long pisoId, Activo activo){
        Activo nombre_activo_repetido = activoRepository.findByNro(activo.getNro());
        if(nombre_activo_repetido != null){
            throw new RuntimeException("El nombre del activo ya existe");
        }
        return this.saveFisico(pisoId, activo);
    }

    @Override
    @Transactional
    public Activo save(Activo activo){
        return activoRepository.save(activo);
    }

    @Override
    @Transactional
    public void deleteById(UUID id){
        activoRepository.deleteById(id);
    }

    @Override
    public Activo findById(UUID id){
        return activoRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Activo no encontrado")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activo> findByPiso(Long pisoId, String search) {
        if (search == null || search.isBlank()) {
            return activoRepository.findByPisoId(pisoId);
        }
        return activoRepository.findByPisoIdAndNroContainingIgnoreCase(pisoId, search);
    }

    @Override
    @Transactional
    public Page<ActivoResponseDTO> listarPorProyectoYEstado(UUID idProyecto, EstadoComercialActivo estado, int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        Page<Activo> activos;
        if(estado == null){
            activos = activoRepository.findByPisoTorreProyectoId(idProyecto,pageable);
        }
        else {
            activos = activoRepository
                    .findByPisoTorreProyectoIdAndEstadoComercial(
                            idProyecto,
                            estado,
                            pageable
                    );
        }
        return activos.map(ActivoResponseDTO::fromEntity);
    }

}
