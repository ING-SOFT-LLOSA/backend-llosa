package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.proyecto.entity.Reporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReporteResponse(

        UUID id,
        UUID uuidProyecto,
        String nombreProyecto,
        String tituloPeriodo,
        BigDecimal porcentajeAvance,
        String descripcion,
        List<String> hitosConsolidados,
        LocalDateTime createdAt,
        List<DocumentoResponse> multimedia

) {

    public static ReporteResponse fromEntity(Reporte reporte, List<DocumentoResponse> multimedia) {
        return new ReporteResponse(
                reporte.getId(),
                reporte.getProyecto().getId(),
                reporte.getProyecto().getNombre(),
                reporte.getTituloPeriodo(),
                reporte.getPorcentajeAvance(),
                reporte.getDescripcion(),
                reporte.getHitosConsolidados(),
                reporte.getCreatedAt(),
                multimedia
        );
    }

    public static ReporteResponse fromEntity(Reporte reporte) {
        return fromEntity(reporte, List.of());
    }
}
