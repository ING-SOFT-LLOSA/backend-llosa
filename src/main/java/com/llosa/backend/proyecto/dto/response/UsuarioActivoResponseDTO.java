package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.seguridad.entity.Usuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para el proceso comercial activo de una unidad inmobiliaria.
 * Incluye la lista completa de copropietarios (clientes) del expediente.
 */
public record UsuarioActivoResponseDTO(
    UUID uuidUsuarioActivo,
    String tipoFinanciamiento,
    LocalDateTime fechaAdquisicion,
    LocalDateTime fechaCompletado,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Boolean vigente,
    List<ClienteSimpleDTO> clientes,
    List<ActivoResponseDTO> activos,
    ClienteSimpleDTO asesor
) {

    /**
     * DTO anidado con la información básica de cada copropietario.
     */
    public record ClienteSimpleDTO(
        Integer id,
        String nombre,
        String apellidos,
        String documentoIdentidad,
        String email,
        String telefono
    ) {
        public static ClienteSimpleDTO fromEntity(Usuario u) {
            return new ClienteSimpleDTO(
                    u.getId(),
                    u.getNombre(),
                    u.getApellidos(),
                    u.getDocumentoIdentidad(),
                    u.getEmail(),
                    u.getTelefono()
            );
        }
    }

    /**
     * Factory method principal: mapea la entidad UsuarioActivo al DTO de respuesta,
     * incluyendo la lista de copropietarios.
     */
    public static UsuarioActivoResponseDTO fromEntity(UsuarioActivo a) {
        List<ClienteSimpleDTO> clientesDTO = a.getClientes() != null
                ? a.getClientes().stream().map(ClienteSimpleDTO::fromEntity).toList()
                : List.of();
        List<ActivoResponseDTO> activos = a.getActivos() != null
                ? a.getActivos().stream().map(ActivoResponseDTO::fromEntity).toList()
                : List.of();
        ClienteSimpleDTO asesor = a.getAsesor() != null
                ? ClienteSimpleDTO.fromEntity(a.getAsesor())
                : null;
        return new UsuarioActivoResponseDTO(
                a.getUuidUsuarioActivo(),
                a.getTipoFinanciamiento(),
                a.getFechaAdquisicion(),
                a.getFechaCompletado(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getVigente(),
                clientesDTO,
                activos,
                asesor
        );
    }
}
