package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.proyecto.dto.request.CrearContratoDTO;
import com.llosa.backend.proyecto.dto.response.UsuarioActivoResponseDTO;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.factory.FlujoComercialFactory;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import com.llosa.backend.proyecto.dto.request.AsignarActivoDTO;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.proyecto.service.ActivoService;
import com.llosa.backend.proyecto.service.UsuarioActivoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioActivoServiceImpl implements UsuarioActivoService {

    private final UsuarioActivoRepository usuarioActivoRepository;
    private final ActivoService activoService;
    private final UsuarioRepository usuarioRepository;
    private final FlujoComercialFactory flujoComercialFactory;
    private final ActivoRepository activoRepository;

    @Override
    @Transactional(readOnly = true)
    public UsuarioActivo findById(UUID id) {
        return usuarioActivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expediente de Usuario-Activo no encontrado: " + id));
    }

    /**
     * Retorna todos los procesos comerciales donde el usuario participa como copropietario.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioActivo> findByUsuario(Integer usuarioId) {
        return usuarioActivoRepository.findByClienteId(usuarioId);
    }

    @Override
    @Transactional
    public UsuarioActivo crearContratoBase(CrearContratoDTO dto){
        // Validar que los clientes existan
        List<Usuario> clientesValidos = dto.idsUsuarios().stream().map(
                id -> usuarioRepository.findById(id).orElseThrow(
                        () -> new EntityNotFoundException("Usuario no encontrado: " + id)
                ))
                .toList();
        // Construimos la entidad
        UsuarioActivo usuarioActivo = UsuarioActivo.builder()
                .tipoFinanciamiento(dto.tipoFinanciamiento())
                .fechaAdquisicion(dto.fechaAdquisicion())
                .clientes(clientesValidos)
                .activos(new ArrayList<>())
                .build();
        List<EtapaExpediente> etapasGeneradas = flujoComercialFactory.generarEtapasPorDefecto(usuarioActivo);
        usuarioActivo.setEtapas(etapasGeneradas);
        return usuarioActivoRepository.save(usuarioActivo);

    }

    @Override
    @Transactional
    public UsuarioActivo save(UsuarioActivo usuarioActivo) {
        return usuarioActivoRepository.save(usuarioActivo);
    }

    /**
     * Crea el proceso comercial activo y vincula la lista de copropietarios recibida en el DTO.
     *
     */
    @Override
    @Transactional
    public UsuarioActivoResponseDTO asignarActivo(AsignarActivoDTO dto) {
        // Encontrar el contrato y validar su existencia
        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(dto.uuidUsuarioActivo()).orElseThrow(
                ()-> new EntityNotFoundException("Usuario no encontrado: " + dto.uuidUsuarioActivo())
        );
        List<Activo> activos = dto.idsActivo().stream().map(
                activoService::findById
        ).toList();
        for (Activo activo : activos) {
            if (!usuarioActivo.equals(activo.getUsuarioActivo()) && activo.getEstadoComercial() == EstadoComercialActivo.DISPONIBLE) {
                activo.setUsuarioActivo(usuarioActivo);
                activo.setEstadoComercial(EstadoComercialActivo.SEPARADO);
                activoRepository.save(activo);
                usuarioActivo.getActivos().add(activo);
            }
        }
        return UsuarioActivoResponseDTO.fromEntity(usuarioActivo);

    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioActivo> findByActivo(UUID activoId) {
        return usuarioActivoRepository.findByActivos_Id(activoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activo> findByUsuarioId(Integer usuarioId){
        return usuarioActivoRepository.findByUsuarioId(usuarioId);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        usuarioActivoRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void eliminarContrato(UUID usuarioActivoId){
        UsuarioActivo usuarioActivo = findById(usuarioActivoId);
        List<Activo> activos = usuarioActivo.getActivos();
        for (Activo activo : activos) {
            activo.setUsuarioActivo(null);
            activo.setEstadoComercial(EstadoComercialActivo.DISPONIBLE);
            activoRepository.save(activo);
        }
        usuarioActivo.setActivos(new ArrayList<>());
        usuarioActivo.setVigente(false);
    }
}
