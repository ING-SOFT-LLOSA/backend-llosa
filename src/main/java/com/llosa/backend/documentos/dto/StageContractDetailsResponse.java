package com.llosa.backend.documentos.dto;

import java.util.List;

public record StageContractDetailsResponse(
    StageDetailsResponse stageDetails
) {
    public record StageDetailsResponse(
        ResumenContratoResponse resumenContrato,
        InformacionContratoResponse informacionContrato
    ) {}

    public record ResumenContratoResponse(
        int totalUnidades,
        String areaTechadaTotal,
        List<UnidadResponse> unidades,
        TotalesResponse totales
    ) {}

    public record UnidadResponse(
        String tipo,
        String nombre,
        String aporteAlContrato,
        String areaOcupada,
        String areaTechada,
        String icono
    ) {}

    public record TotalesResponse(
        int departamentos,
        int estacionamientos
    ) {}

    public record InformacionContratoResponse(
        String firmaContrato,
        String fechaDesembolso,
        String modalidadPago
    ) {}
}
