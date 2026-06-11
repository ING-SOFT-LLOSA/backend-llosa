package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.dto.EtapaExpedienteEstadoRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteRequest;
import com.llosa.backend.comercial.dto.EtapaExpedienteResponse;

import java.util.List;
import java.util.UUID;

public interface EtapaExpedienteService {
    List<EtapaExpedienteResponse> listarPorUsuarioActivo(UUID uuidUsuarioActivo);
    EtapaExpedienteResponse obtenerPorId(UUID uuidEtapaExpediente);
    EtapaExpedienteResponse crear(UUID uuidUsuarioActivo, EtapaExpedienteRequest request);
    EtapaExpedienteResponse actualizar(UUID uuidEtapaExpediente, EtapaExpedienteRequest request);
    EtapaExpedienteResponse actualizarEstado(UUID uuidEtapaExpediente, EtapaExpedienteEstadoRequest request);
    void eliminar(UUID uuidEtapaExpediente);
}
