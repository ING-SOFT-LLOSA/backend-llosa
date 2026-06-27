package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.EntidadDuplicadaException;
import com.llosa.backend.proyecto.dto.response.ActivoResponseDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.repository.ActivoRepository;

import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.PisoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivoServiceImpl implements ActivoService {

    private final ActivoRepository activoRepository;
    private final PisoService pisoService;
    private final UsuarioActivoRepository usuarioActivoRepository;

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
        Activo nombreActivoRepetido = activoRepository.findByNro(activo.getNro());
        if(nombreActivoRepetido != null){
            throw new EntidadDuplicadaException("El nombre del activo ya existe");
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
        Optional<UsuarioActivo> expedienteAsociado = usuarioActivoRepository.findByActivos_Id(id);
        if (expedienteAsociado.isPresent()) {
            throw new BusinessException("No se puede eliminar la unidad porque está asociada al expediente comercial activo del cliente.");
        }
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
