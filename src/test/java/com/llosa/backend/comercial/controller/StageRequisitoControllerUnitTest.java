package com.llosa.backend.comercial.controller;

import com.llosa.backend.comercial.dto.RequisitoCreateRequest;
import com.llosa.backend.comercial.dto.RequisitoUpdateRequest;
import com.llosa.backend.comercial.dto.StageActivosResponse;
import com.llosa.backend.comercial.dto.StageDocumentsResponse;
import com.llosa.backend.comercial.dto.StageTrackerResponse;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.enums.EtapaRequisitoDocumental;
import com.llosa.backend.comercial.service.RequisitoDocumentalService;
import com.llosa.backend.comercial.service.impl.StageServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de StageController y RequisitoDocumentalController (sin Docker):
 * servicio mockeado + Authentication mockeada. Cubren clases que el CI tenía al 0%.
 */
class StageRequisitoControllerUnitTest {

    private Authentication authConUid(String uid) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(uid);
        return auth;
    }

    private RequisitoDocumental requisitoDummy() {
        EtapaExpediente etapa = EtapaExpediente.builder()
                .uuidEtapaExpediente(UUID.randomUUID()).build();
        return RequisitoDocumental.builder()
                .id(UUID.randomUUID())
                .etapaExpediente(etapa)
                .titulo("Minuta")
                .estado(EtapaRequisitoDocumental.PENDIENTE)
                .build();
    }

    // ── StageController ───────────────────────────────────────────────────────

    @Test
    void stage_obtenerStage_documents_activos_devuelven200() {
        StageServiceImpl stageService = mock(StageServiceImpl.class);
        StageController controller = new StageController(stageService);
        UUID expediente = UUID.randomUUID();
        Authentication auth = authConUid("uid-gestor");

        StageTrackerResponse tracker = mock(StageTrackerResponse.class);
        when(stageService.obtenerStage("uid-gestor", expediente, EtapaProceso.CONTRATO)).thenReturn(tracker);
        assertThat(controller.obtenerStage(EtapaProceso.CONTRATO, expediente, auth).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        StageDocumentsResponse docs = mock(StageDocumentsResponse.class);
        when(stageService.obtenerDocumentosStage("uid-gestor", expediente, EtapaProceso.CONTRATO)).thenReturn(docs);
        assertThat(controller.obtenerDocumentosStage(EtapaProceso.CONTRATO, expediente, auth).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        StageActivosResponse activos = mock(StageActivosResponse.class);
        when(stageService.obtenerActivosEtapa("uid-gestor", expediente, EtapaProceso.CONTRATO)).thenReturn(activos);
        assertThat(controller.obtenerActivosStage(EtapaProceso.CONTRATO, expediente, auth).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    // ── RequisitoDocumentalController ─────────────────────────────────────────

    @Test
    void requisito_subirArchivo_devuelve200YDelega() {
        RequisitoDocumentalService service = mock(RequisitoDocumentalService.class);
        RequisitoDocumentalController controller = new RequisitoDocumentalController(service);
        UUID requisitoId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "minuta.pdf",
                "application/pdf", "contenido".getBytes());

        when(service.asociarArchivoARequisito(eq(requisitoId), any(), eq("uid-x")))
                .thenReturn(requisitoDummy());

        ResponseEntity<?> resp = controller.subirArchivoRequisito(requisitoId, file, authConUid("uid-x"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void requisito_eliminarArchivo_devuelve204YDelega() {
        RequisitoDocumentalService service = mock(RequisitoDocumentalService.class);
        RequisitoDocumentalController controller = new RequisitoDocumentalController(service);
        UUID requisitoId = UUID.randomUUID();

        ResponseEntity<Void> resp = controller.eliminarArchivoRequisito(requisitoId, authConUid("uid-x"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).eliminarArchivoDeRequisito(requisitoId, "uid-x");
    }

    @Test
    void requisito_crear_devuelve201() {
        RequisitoDocumentalService service = mock(RequisitoDocumentalService.class);
        RequisitoDocumentalController controller = new RequisitoDocumentalController(service);
        RequisitoCreateRequest req = mock(RequisitoCreateRequest.class);
        when(service.crearRequisito(req)).thenReturn(requisitoDummy());

        assertThat(controller.crearRequisito(req).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void requisito_actualizar_devuelve200() {
        RequisitoDocumentalService service = mock(RequisitoDocumentalService.class);
        RequisitoDocumentalController controller = new RequisitoDocumentalController(service);
        UUID id = UUID.randomUUID();
        RequisitoUpdateRequest req = mock(RequisitoUpdateRequest.class);
        when(service.actualizarRequisito(id, req)).thenReturn(requisitoDummy());

        assertThat(controller.actualizarRequisito(id, req).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void requisito_eliminarTotalmente_devuelve204YDelega() {
        RequisitoDocumentalService service = mock(RequisitoDocumentalService.class);
        RequisitoDocumentalController controller = new RequisitoDocumentalController(service);
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> resp = controller.eliminarRequisitoTotalmente(id, authConUid("uid-x"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).eliminarRequisitoTotalmente(id, "uid-x");
    }
}
