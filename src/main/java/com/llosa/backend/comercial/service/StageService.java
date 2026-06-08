package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.dto.StageDocumentResponse;
import com.llosa.backend.comercial.dto.StageResponse;
import com.llosa.backend.comercial.enums.EtapaProceso;

import java.util.UUID;

public interface StageService {
    StageResponse obtenerStage(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso);
    StageDocumentResponse obtenerDocumentosStage(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso);
}
