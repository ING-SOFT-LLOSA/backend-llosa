package com.llosa.backend.comercial.dto;

import com.llosa.backend.comercial.enums.EstadoEtapaExpediente;
import com.llosa.backend.comercial.enums.EtapaProceso;
import jakarta.validation.constraints.NotNull;

public record EtapaExpedienteRequest(
        @NotNull(message = "La etapa del proceso es obligatoria")
        EtapaProceso etapaProceso,
        EstadoEtapaExpediente estado
) {}