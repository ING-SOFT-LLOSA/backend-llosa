package com.llosa.backend.comercial.dto;

import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.enums.EstadoEtapaExpediente;
import com.llosa.backend.comercial.enums.EtapaProceso;

import java.util.UUID;

public record EtapaExpedienteResponse(
        UUID uuidEtapaExpediente,
        UUID uuidUsuarioActivo,
        EtapaProceso etapaProceso,
        EstadoEtapaExpediente estado,
        Integer totalHitos,
        Integer totalRequisitos
) {
    public static EtapaExpedienteResponse fromEntity(EtapaExpediente e) {
        return new EtapaExpedienteResponse(
                e.getUuidEtapaExpediente(),
                e.getUsuarioActivo().getUuidUsuarioActivo(),
                e.getEtapaProceso(),
                e.getEstado(),
                e.getHitosComerciales() != null ? e.getHitosComerciales().size() : 0,
                e.getRequisitos() != null ? e.getRequisitos().size() : 0
        );
    }
}
