package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.dto.request.CrearContratoDTO;
import com.llosa.backend.proyecto.dto.response.UsuarioActivoResponseDTO;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.factory.FlujoComercialFactory;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final UsuarioRepository usuarioRepository;
    private final FlujoComercialFactory flujoComercialFactory;
    private final ActivoRepository activoRepository;

    private static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado: ";

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
                        () -> new EntityNotFoundException(USUARIO_NO_ENCONTRADO + id)
                ))
                .toList();
        // Construimos la entidad
        UsuarioActivo usuarioActivo = UsuarioActivo.builder()
                .tipoFinanciamiento(dto.tipoFinanciamiento())
                .fechaAdquisicion(dto.fechaAdquisicion())
                .fechaCompletado(dto.fechaCompletado())
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
                ()-> new EntityNotFoundException(USUARIO_NO_ENCONTRADO + dto.uuidUsuarioActivo())
        );
        List<Activo> activos = activoRepository.findByIdsForUpdate(dto.idsActivo());

        if (activos.size() != dto.idsActivo().size()) {
            throw new EntityNotFoundException("Uno o más activos solicitados no existen.");
        }
        for (Activo activo : activos) {
            if (activo.getEstadoComercial() != EstadoComercialActivo.DISPONIBLE) {
                throw new BusinessException("El activo '" + activo.getNro() + "' ya no está disponible (Estado actual: " + activo.getEstadoComercial() + ").");
            }
            activo.setUsuarioActivo(usuarioActivo);
            activo.setEstadoComercial(EstadoComercialActivo.SEPARADO);

            activoRepository.save(activo);
            usuarioActivo.getActivos().add(activo);
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
        // 1. Buscamos el expediente completo con sus relaciones (clientes y activos)
        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expediente de Usuario-Activo no encontrado: " + id));

        // 2. REGLA DE NEGOCIO 1: Liberar los activos (Volver a DISPONIBLE)
        if (usuarioActivo.getActivos() != null) {
            for (Activo activo : usuarioActivo.getActivos()) {
                activo.setUsuarioActivo(null);
                activo.setEstadoComercial(EstadoComercialActivo.DISPONIBLE);
                activoRepository.save(activo); // Persistimos el cambio de estado en el activo
            }
            // Limpiamos la lista en memoria para evitar problemas de persistencia en cascada
            usuarioActivo.getActivos().clear();
        }

        // 3. REGLA DE NEGOCIO 2: Desactivar a los clientes vinculados si era su única unidad
        if (usuarioActivo.getClientes() != null) {
            for (Usuario cliente : usuarioActivo.getClientes()) {
                // Buscamos si este cliente tiene OTROS contratos vigentes en el sistema
                List<UsuarioActivo> otrosContratos = usuarioActivoRepository.findByClienteId(cliente.getId());

                // Si el único contrato que tenía era este (o ninguno más está vigente), lo desactivamos
                // Nota: Filtramos para no contar el contrato actual que estamos destruyendo
                long contratosActivosRestantes = otrosContratos.stream()
                        .filter(c -> !c.getUuidUsuarioActivo().equals(id))
                        .count();

                if (contratosActivosRestantes == 0) {
                    // Cambiamos el estado del perfil del cliente a Inactivo
                    // Ajusta 'setActivo(false)' o 'setEstado(...)' según las propiedades reales de tu entidad Usuario
                    cliente.setActivo(false);
                    usuarioRepository.save(cliente);
                }
            }
        }

        usuarioActivoRepository.delete(usuarioActivo);
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

    @Override
    @Transactional
    public Page<UsuarioActivoResponseDTO> listar(Pageable pageable){
        return usuarioActivoRepository.findAll(pageable).map(UsuarioActivoResponseDTO::fromEntity);
    }

    @Override
    @Transactional
    public UsuarioActivo asignarAsesorAContrato(UUID idUsuarioActivo, Integer idUsuario){
        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(idUsuarioActivo).orElseThrow(
                () -> new EntityNotFoundException(USUARIO_NO_ENCONTRADO + idUsuarioActivo)
        );
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow(
                () -> new EntityNotFoundException(USUARIO_NO_ENCONTRADO + idUsuario)
        );
        usuarioActivo.setAsesor(usuario);

        return usuarioActivoRepository.save(usuarioActivo);
    }

    @Override
    @Transactional
    public UsuarioActivo desasignarAsesorDelContrato(UUID idUsuarioActivo,Integer idUsuario){
        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(idUsuarioActivo).orElseThrow(
                () -> new EntityNotFoundException(USUARIO_NO_ENCONTRADO + idUsuarioActivo)
        );
        usuarioActivo.setAsesor(null);

        return usuarioActivoRepository.save(usuarioActivo);
    }

}
