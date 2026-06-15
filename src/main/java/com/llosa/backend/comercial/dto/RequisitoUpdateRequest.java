package com.llosa.backend.comercial.dto;

import com.llosa.backend.comercial.enums.EtapaRequisitoDocumental;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record RequisitoUpdateRequest(
        @NotBlank String titulo,
        String descripcion,
        String notaCorporativa,
        EtapaRequisitoDocumental estado, // "PENDIENTE", "COMPLETADA", "RECHAZADO"
        LocalDate fechaEmision,
        String icono
) {}