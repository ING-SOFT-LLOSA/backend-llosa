package com.llosa.backend.documentos.controller;

import com.llosa.backend.comercial.dto.StageDocumentResponse;
import com.llosa.backend.comercial.dto.StageResponse;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.service.impl.StageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del controlador REST del Stage del Portal del Cliente
 * (CP41/CP44). Verifica el contrato HTTP y el reenvío del UID autenticado.
 */
@ExtendWith(MockitoExtension.class)
class StageControllerTest {

    @Mock StageServiceImpl stageService;
    @Mock Authentication authentication;

    @InjectMocks StageController controller;

    private static final String UID = "firebase-uid";

    @Test
    void obtenerStage_devuelve200() {
        UUID uaId = UUID.randomUUID();
        StageResponse resp = new StageResponse(
                new StageResponse.StageInfo(uaId.toString(), "Contrato", 2, 5, 50.0),
                List.of(), null);
        when(authentication.getPrincipal()).thenReturn(UID);
        when(stageService.obtenerStage(UID, uaId, EtapaProceso.CONTRATO)).thenReturn(resp);

        ResponseEntity<StageResponse> r =
                controller.obtenerStage(EtapaProceso.CONTRATO, uaId, authentication);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo(resp);
    }

    @Test
    void obtenerDocumentosStage_devuelve200() {
        UUID uaId = UUID.randomUUID();
        StageDocumentResponse resp = new StageDocumentResponse("Documentos del Contrato", 0, List.of());
        when(authentication.getPrincipal()).thenReturn(UID);
        when(stageService.obtenerDocumentosStage(UID, uaId, EtapaProceso.CONTRATO)).thenReturn(resp);

        ResponseEntity<StageDocumentResponse> r =
                controller.obtenerDocumentosStage(EtapaProceso.CONTRATO, uaId, authentication);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageService).obtenerDocumentosStage(UID, uaId, EtapaProceso.CONTRATO);
    }
}
