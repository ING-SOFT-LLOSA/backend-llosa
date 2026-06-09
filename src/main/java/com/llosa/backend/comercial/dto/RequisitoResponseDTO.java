package com.llosa.backend.comercial.dto;

import com.llosa.backend.comercial.entity.RequisitoDocumental;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record RequisitoResponseDTO(
        @NotNull UUID id,
        @NotNull UUID hitoProcesoCompraId,
        @NotBlank String titulo,
        String descripcion,
        String notaCorporativa,
        String estado,
        LocalDate fechaEmision,
        String icono
) {
    public static RequisitoResponseDTO fromEntity(RequisitoDocumental r) {
        return new RequisitoResponseDTO(
                r.getId(),
                r.getHitoComercial().getUuidHitoComercial(),
                r.getTitulo(),
                r.getDescripcion(),
                r.getNotaCorporativa(),
                r.getEstado(),
                r.getFechaEmision(),
                r.getIcono()
        );
    }
}
