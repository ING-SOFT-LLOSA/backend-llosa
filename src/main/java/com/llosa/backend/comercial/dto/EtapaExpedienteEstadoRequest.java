package com.llosa.backend.comercial.dto;

import com.llosa.backend.comercial.enums.EstadoEtapaExpediente;

import jakarta.validation.constraints.NotNull;

public record EtapaExpedienteEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoEtapaExpediente estado
) {}