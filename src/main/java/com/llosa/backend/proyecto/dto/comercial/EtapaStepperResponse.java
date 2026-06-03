package com.llosa.backend.proyecto.dto.comercial;

import com.llosa.backend.proyecto.entity.comercial.EtapaProceso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa una etapa dentro del stepper de proceso de compra,
 * con sus hitos asociados y porcentaje de avance calculado dinámicamente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapaStepperResponse {

    private EtapaProceso etapa;
    private List<HitoComercialResponse> hitos;
    private double porcentajeAvance;
}
