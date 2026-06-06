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
    String faseComercial,
    String estadoTramiteLegal,
    LocalDateTime fechaAdquisicion,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<ClienteSimpleDTO> clientes,
    ActivoResponseDTO activo
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
        List<ClienteSimpleDTO> clientesDTO = a.getClientes().stream()
                .map(ClienteSimpleDTO::fromEntity)
                .toList();

        return new UsuarioActivoResponseDTO(
                a.getUuidUsuarioActivo(),
                a.getTipoFinanciamiento(),
                a.getFaseComercial(),
                a.getEstadoTramiteLegal(),
                a.getFechaAdquisicion(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                clientesDTO,
                ActivoResponseDTO.fromEntity(a.getActivo())
        );
    }
}
