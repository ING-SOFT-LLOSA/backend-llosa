package com.llosa.backend.documentos.dto;

import com.llosa.backend.documentos.enums.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DtoTest {

    @Test
    void testDocumentoResponse() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        DocumentoResponse response = new DocumentoResponse(
                id, "test.pdf", TipoDocumento.PDF_LEGAL, "application/pdf",
                "ref1", "PROYECTO", now, "url"
        );

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.nombreOriginal()).isEqualTo("test.pdf");
        assertThat(response.tipoDocumento()).isEqualTo(TipoDocumento.PDF_LEGAL);
        assertThat(response.tipoMime()).isEqualTo("application/pdf");
        assertThat(response.idReferencia()).isEqualTo("ref1");
        assertThat(response.entidadReferencia()).isEqualTo("PROYECTO");
        assertThat(response.urlAcceso()).isEqualTo("url");
    }

    @Test
    void testSignedUrlResponse() {
        Instant now = Instant.now();
        SignedUrlResponse response = new SignedUrlResponse("url", now);

        assertThat(response.url()).isEqualTo("url");
        assertThat(response.expiracion()).isEqualTo(now);
    }

    @Test
    void testSubirDocumentoRequest() {
        SubirDocumentoRequest request = new SubirDocumentoRequest(TipoDocumento.PDF_LEGAL);
        assertThat(request.tipoDocumento()).isEqualTo(TipoDocumento.PDF_LEGAL);
    }
    
    @Test
    void testStageContractDetailsResponse() {
        StageContractDetailsResponse.TotalesResponse totales = new StageContractDetailsResponse.TotalesResponse(1, 1);
        StageContractDetailsResponse.UnidadResponse unidad = new StageContractDetailsResponse.UnidadResponse(
                "Dpto", "Dpto 1", "aporte", "areaOcc", "areaTech", "icono"
        );
        StageContractDetailsResponse.ResumenContratoResponse resumen = new StageContractDetailsResponse.ResumenContratoResponse(
                2, "areaTotal", java.util.List.of(unidad), totales
        );
        StageContractDetailsResponse.InformacionContratoResponse info = new StageContractDetailsResponse.InformacionContratoResponse(
                "firma", "desembolso", "modalidad"
        );
        StageContractDetailsResponse.StageDetailsResponse details = new StageContractDetailsResponse.StageDetailsResponse(resumen, info);
        StageContractDetailsResponse response = new StageContractDetailsResponse(details);

        assertThat(response.stageDetails()).isEqualTo(details);
        assertThat(response.stageDetails().resumenContrato()).isEqualTo(resumen);
        assertThat(response.stageDetails().informacionContrato()).isEqualTo(info);
    }
}
