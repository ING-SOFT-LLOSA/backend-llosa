package com.llosa.backend.comercial.service.impl;

import com.llosa.backend.comercial.dto.EtapaExpedienteEstadoRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteResponse;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.enums.EstadoEtapaExpediente;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.comercial.service.EtapaExpedienteService;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/* Falta testear*/
@Service
@RequiredArgsConstructor
public class EtapaExpedienteServiceImpl implements EtapaExpedienteService {

    private final EtapaExpedienteRepository etapaExpedienteRepository;
    private final UsuarioActivoRepository usuarioActivoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EtapaExpedienteResponse> listarPorUsuarioActivo(UUID uuidUsuarioActivo) {
        return etapaExpedienteRepository
                .findByUsuarioActivo_UuidUsuarioActivoOrderByEtapaProcesoAsc(uuidUsuarioActivo)
                .stream()
                .map(EtapaExpedienteResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EtapaExpedienteResponse obtenerPorId(UUID uuidEtapaExpediente) {
        EtapaExpediente etapa = etapaExpedienteRepository.findById(uuidEtapaExpediente)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Etapa expediente no encontrada: " + uuidEtapaExpediente
                ));
        return EtapaExpedienteResponse.fromEntity(etapa);
    }

    @Override
    @Transactional
    public EtapaExpedienteResponse crear(UUID uuidUsuarioActivo, EtapaExpedienteRequest request) {
        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(uuidUsuarioActivo)
                .orElseThrow(() -> new EntityNotFoundException(
                        "UsuarioActivo no encontrado: " + uuidUsuarioActivo
                ));

        if (etapaExpedienteRepository.existsByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(
                uuidUsuarioActivo, request.etapaProceso())) {
            throw new IllegalStateException(
                    "Ya existe una etapa " + request.etapaProceso() +
                            " para este expediente"
            );
        }

        EtapaExpediente etapa = EtapaExpediente.builder()
                .usuarioActivo(usuarioActivo)
                .etapaProceso(request.etapaProceso())
                .estado(request.estado() != null ? request.estado() : EstadoEtapaExpediente.PENDIENTE)
                .build();

        EtapaExpediente guardada = etapaExpedienteRepository.save(etapa);
        return EtapaExpedienteResponse.fromEntity(guardada);
    }

    @Override
    @Transactional
    public EtapaExpedienteResponse actualizar(UUID uuidEtapaExpediente, EtapaExpedienteRequest request) {
        EtapaExpediente etapa = etapaExpedienteRepository.findById(uuidEtapaExpediente)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Etapa expediente no encontrada: " + uuidEtapaExpediente
                ));

        etapa.setEtapaProceso(request.etapaProceso());
        if (request.estado() != null) {
            etapa.setEstado(request.estado());
        }

        EtapaExpediente actualizada = etapaExpedienteRepository.save(etapa);
        return EtapaExpedienteResponse.fromEntity(actualizada);
    }

    @Override
    @Transactional
    public EtapaExpedienteResponse actualizarEstado(UUID uuidEtapaExpediente, EtapaExpedienteEstadoRequest request) {
        EtapaExpediente etapa = etapaExpedienteRepository.findById(uuidEtapaExpediente)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Etapa expediente no encontrada: " + uuidEtapaExpediente
                ));

        etapa.setEstado(request.estado());

        EtapaExpediente actualizada = etapaExpedienteRepository.save(etapa);
        return EtapaExpedienteResponse.fromEntity(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(UUID uuidEtapaExpediente) {
        if (!etapaExpedienteRepository.existsById(uuidEtapaExpediente)) {
            throw new EntityNotFoundException(
                    "Etapa expediente no encontrada: " + uuidEtapaExpediente
            );
        }
        etapaExpedienteRepository.deleteById(uuidEtapaExpediente);
    }
}
