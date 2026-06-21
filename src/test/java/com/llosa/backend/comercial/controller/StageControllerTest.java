package com.llosa.backend.comercial.controller;

import com.llosa.backend.comercial.dto.StageActivosResponse;
import com.llosa.backend.comercial.dto.StageDocumentsResponse;
import com.llosa.backend.comercial.dto.StageTrackerResponse;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.service.impl.StageServiceImpl;
import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StageController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class StageControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean StageServiceImpl stageService;
    @MockitoBean FirebaseConfig firebaseConfig;
    @MockitoBean com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    @Test
    void obtenerStage_sinAutenticar_devuelve403() throws Exception {
        mockMvc.perform(get("/api/stage/CONTRATO")
                        .param("uuidUsuarioActivo", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerStage_autenticado_devuelveTracker() throws Exception {
        UUID usuarioActivoId = UUID.randomUUID();
        StageTrackerResponse response = new StageTrackerResponse(
                new StageTrackerResponse.StageInfo("CONTRATO", "Contrato", 1, 3, 33.3),
                List.of(),
                null);
        when(stageService.obtenerStage("test-uid", usuarioActivoId, EtapaProceso.CONTRATO))
                .thenReturn(response);

        mockMvc.perform(get("/api/stage/CONTRATO")
                        .param("uuidUsuarioActivo", usuarioActivoId.toString())
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage.id").value("CONTRATO"));
    }

    @Test
    void obtenerDocumentosStage_autenticado_devuelveDocumentos() throws Exception {
        UUID usuarioActivoId = UUID.randomUUID();
        StageDocumentsResponse response = new StageDocumentsResponse("Documentos", 0, List.of());
        when(stageService.obtenerDocumentosStage("test-uid", usuarioActivoId, EtapaProceso.CONTRATO))
                .thenReturn(response);

        mockMvc.perform(get("/api/stage/CONTRATO/documents")
                        .param("uuidUsuarioActivo", usuarioActivoId.toString())
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Documentos"));
    }

    @Test
    void obtenerActivosStage_autenticado_devuelveActivos() throws Exception {
        UUID usuarioActivoId = UUID.randomUUID();
        StageActivosResponse response = new StageActivosResponse("Resumen", 0, List.of());
        when(stageService.obtenerActivosEtapa("test-uid", usuarioActivoId, EtapaProceso.CONTRATO))
                .thenReturn(response);

        mockMvc.perform(get("/api/stage/CONTRATO/activos")
                        .param("uuidUsuarioActivo", usuarioActivoId.toString())
                        .with(authentication(TestData.proyectoAuthToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen").value("Resumen"));
    }
}
