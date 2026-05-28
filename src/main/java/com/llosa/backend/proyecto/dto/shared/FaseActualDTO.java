package com.llosa.backend.proyecto.dto.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record FaseActualDTO(
        @JsonProperty("uuid_hito_u") UUID uuidHitoU,
        String titulo,
        String descripcion,
        @JsonProperty("porcentaje_etapa") double porcentajeEtapa,
        @JsonProperty("fecha_inicio_fase") LocalDateTime fechaInicioFase
) {
}
