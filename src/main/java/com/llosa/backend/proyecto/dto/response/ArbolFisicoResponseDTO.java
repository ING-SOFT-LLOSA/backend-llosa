package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.enums.TipoActivo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Árbol Proyecto → Torres → Pisos → Activos.
 * Usa inner records para evitar referencias circulares en la serialización.
 */
public record ArbolFisicoResponseDTO(
        UUID id,
        String nombre,
        String distrito,
        String direccion,
        List<TorreItem> torres
) {

    public record TorreItem(
            Long id,
            String nombre,
            List<PisoItem> pisos
    ) {
        public static TorreItem fromEntity(Torre t) {
            return new TorreItem(
                    t.getId(),
                    t.getNombre(),
                    t.getPisos().stream().map(PisoItem::fromEntity).toList()
            );
        }
    }

    public record PisoItem(
            Long id,
            Integer nroPiso,
            List<ActivoItem> activos
    ) {
        public static PisoItem fromEntity(Piso p) {
            return new PisoItem(
                    p.getId(),
                    p.getNroPiso(),
                    p.getActivos().stream().map(ActivoItem::fromEntity).toList()
            );
        }
    }

    public record ActivoItem(
            UUID id,
            String nro,
            TipoActivo tipo,
            BigDecimal precio,
            String descripcion
    ) {
        public static ActivoItem fromEntity(Activo a) {
            return new ActivoItem(
                    a.getId(),
                    a.getNro(),
                    a.getTipo(),
                    a.getPrecio(),
                    a.getDescripcion()
            );
        }
    }

    public static ArbolFisicoResponseDTO fromEntity(Proyecto p) {
        return new ArbolFisicoResponseDTO(
                p.getId(),
                p.getNombre(),
                p.getDistrito(),
                p.getDireccion(),
                p.getTorres().stream().map(TorreItem::fromEntity).toList()
        );
    }
}
