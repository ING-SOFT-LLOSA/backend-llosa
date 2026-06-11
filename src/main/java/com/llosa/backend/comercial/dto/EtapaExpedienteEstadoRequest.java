package com.llosa.backend.comercial.dto;

import com.llosa.backend.comercial.enums.EstadoEtapaExpediente;
import jakarta.validation.constraints.NotBlank;

public record EtapaExpedienteEstadoRequest(
        @NotBlank(message = "El estado es obligatorio")
        EstadoEtapaExpediente estado
) {}