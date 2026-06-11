package com.llosa.backend.proyecto.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para registrar un proceso comercial activo.
 * Soporta múltiples clientes (copropietarios) desde el momento de la asignación.
 */
public record AsignarActivoDTO(
        @NotNull UUID uuidUsuarioActivo, // El contrato al que se lo vas a colgar
        @NotNull List<UUID> idsActivo           // El departamento, cochera o depósito que se suma
) {}
