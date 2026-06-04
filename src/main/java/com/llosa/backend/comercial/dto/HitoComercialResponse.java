package com.llosa.backend.comercial.dto;

import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida con la representación simple de un hito comercial.
 */
@Builder
public record HitoComercialResponse(
    UUID uuidHitoComercial,
    UUID uuidUsuarioActivo,
    EtapaProceso etapaProceso,
    String nombreHito,
    String descripcion,
    Integer orden,
    EstadoHitoComercial estado,
    LocalDateTime fechaCompletado,
    LocalDateTime createdAt
) {
    /**
     * Factory method para mapear desde la entidad al DTO de respuesta.
     */
    public static HitoComercialResponse fromEntity(HitoProcesoCompra entity) {
        return HitoComercialResponse.builder()
                .uuidHitoComercial(entity.getUuidHitoComercial())
                .uuidUsuarioActivo(entity.getUsuarioActivo().getUuidUsuarioActivo())
                .etapaProceso(entity.getEtapaProceso())
                .nombreHito(entity.getNombreHito())
                .descripcion(entity.getDescripcion())
                .orden(entity.getOrden())
                .estado(entity.getEstado())
                .fechaCompletado(entity.getFechaCompletado())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
