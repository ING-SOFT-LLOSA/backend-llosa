package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.dto.StageActivosResponse;
import com.llosa.backend.comercial.dto.StageDocumentsResponse;
import com.llosa.backend.comercial.dto.StageTrackerResponse;
import com.llosa.backend.comercial.enums.EtapaProceso;

import java.util.UUID;

public interface StageService {
    StageTrackerResponse obtenerStage(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso);
    StageDocumentsResponse obtenerDocumentosStage(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso);
    StageActivosResponse obtenerActivosEtapa(String firebaseUid, UUID uuidUsuarioActivo, EtapaProceso etapaProceso);
}
