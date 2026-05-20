package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.Etapa;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.enums.EstadoEtapa;
import com.llosa.backend.proyecto.enums.EstadoHito;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Árbol Proyecto → Etapas → Hitos.
 * Usa inner records para evitar referencias circulares en la serialización.
 */
public record CronogramaResponseDTO(
        UUID id,
        String nombre,
        String distrito,
        List<EtapaItem> etapas
) {

    public record EtapaItem(
            Long id,
            String nombre,
            String descripcion,
            EstadoEtapa estado,
            List<HitoItem> hitos
    ) {
        public static EtapaItem fromEntity(Etapa e) {
            return new EtapaItem(
                    e.getId(),
                    e.getNombre(),
                    e.getDescripcion(),
                    e.getEstado(),
                    e.getHitos().stream().map(HitoItem::fromEntity).toList()
            );
        }
    }

    public record HitoItem(
            UUID id,
            String titulo,
            String nombre,
            String descripcion,
            EstadoHito estado,
            LocalDate fechaEstimada
    ) {
        public static HitoItem fromEntity(Hito h) {
            return new HitoItem(
                    h.getId(),
                    h.getTitulo(),
                    h.getNombre(),
                    h.getDescripcion(),
                    h.getEstado(),
                    h.getFechaEstimada()
            );
        }
    }

    public static CronogramaResponseDTO fromEntity(Proyecto p) {
        return new CronogramaResponseDTO(
                p.getId(),
                p.getNombre(),
                p.getDistrito(),
                p.getEtapas().stream().map(EtapaItem::fromEntity).toList()
        );
    }
}
