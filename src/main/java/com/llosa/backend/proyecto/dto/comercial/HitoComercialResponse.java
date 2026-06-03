package com.llosa.backend.proyecto.dto.comercial;

import com.llosa.backend.proyecto.entity.comercial.EstadoHitoComercial;
import com.llosa.backend.proyecto.entity.comercial.EtapaProceso;
import com.llosa.backend.proyecto.entity.comercial.HitoProcesoCompra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida con la representación simple de un hito comercial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HitoComercialResponse {

    private UUID uuidHitoComercial;
    private UUID uuidUsuarioActivo;
    private EtapaProceso etapaProceso;
    private String nombreHito;
    private String descripcion;
    private Integer orden;
    private EstadoHitoComercial estado;
    private LocalDateTime fechaCompletado;
    private LocalDateTime createdAt;

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
