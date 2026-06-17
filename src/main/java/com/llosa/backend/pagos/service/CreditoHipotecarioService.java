package com.llosa.backend.pagos.service;

import com.llosa.backend.pagos.dto.CreditoHipotecarioResponse;

import java.util.UUID;

public interface CreditoHipotecarioService {

    CreditoHipotecarioResponse obtenerResumen(UUID uuidUsuarioActivo, String firebaseUid);
}
