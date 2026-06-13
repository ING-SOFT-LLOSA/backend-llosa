package com.llosa.backend.pagos.dto;

import com.llosa.backend.proyecto.dto.response.UsuarioActivoResponseDTO;

import java.util.UUID;

public record ContratoDetalleResponse(
        UsuarioActivoResponseDTO expediente,
        CronogramaPagoResponse cronograma,
        CartaAprobacionResponse cartaAprobacion,
        ResumenResponse resumen
) {}
