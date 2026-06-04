package com.llosa.backend.proyecto.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para registrar un proceso comercial activo.
 * Soporta múltiples clientes (copropietarios) desde el momento de la asignación.
 */
public record AsignarActivoDTO(
        @NotEmpty List<Integer> idsUsuarios,   // Lista de IDs de todos los copropietarios
        @NotNull UUID idActivo,
        String tipoFinanciamiento,
        String faseComercial,
        String estadoTramiteLegal,
        LocalDateTime fechaAdquisicion
) {}
