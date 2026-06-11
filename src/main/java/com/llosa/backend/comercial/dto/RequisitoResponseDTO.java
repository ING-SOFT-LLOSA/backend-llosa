package com.llosa.backend.comercial.dto;

import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EtapaRequisitoDocumental;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record RequisitoResponseDTO(
        @NotNull UUID id,
        @NotNull UUID etapaProcesoCompraId,
        @NotBlank String titulo,
        String descripcion,
        String notaCorporativa,
        EtapaRequisitoDocumental estado,
        LocalDate fechaEmision,
        String icono
) {
    public static RequisitoResponseDTO fromEntity(RequisitoDocumental r) {
        return new RequisitoResponseDTO(
                r.getId(),
                r.getEtapaExpediente().getUuidEtapaExpediente(),
                r.getTitulo(),
                r.getDescripcion(),
                r.getNotaCorporativa(),
                r.getEstado(),
                r.getFechaEmision(),
                r.getIcono()
        );
    }
}
