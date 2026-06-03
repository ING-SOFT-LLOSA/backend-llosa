package com.llosa.backend.proyecto.dto.response;

import com.llosa.backend.proyecto.entity.comercial.EtapaProceso;
import lombok.Builder;

import java.util.List;

/**
 * DTO que representa una etapa dentro del stepper de proceso de compra,
 * con sus hitos asociados y porcentaje de avance calculado dinámicamente.
 */
@Builder
public record EtapaStepperResponse(
    EtapaProceso etapa,
    List<HitoComercialResponse> hitos,
    double porcentajeAvance
) {}
