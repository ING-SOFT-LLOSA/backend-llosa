package com.llosa.backend.proyecto.service.comercial;

import com.llosa.backend.proyecto.dto.request.HitoComercialRequest;
import com.llosa.backend.proyecto.dto.response.HitoComercialResponse;
import com.llosa.backend.proyecto.dto.response.StepperResponse;
import com.llosa.backend.proyecto.entity.comercial.EstadoHitoComercial;

import java.util.UUID;

/**
 * Contrato del servicio de Hitos Comerciales (Proceso de Compra).
 */
public interface HitoComercialService {

    HitoComercialResponse crearHito(HitoComercialRequest request);

    void eliminarHito(UUID uuidHitoComercial);

    HitoComercialResponse actualizarEstado(UUID uuidHitoComercial, EstadoHitoComercial nuevoEstado);

    StepperResponse obtenerStepper(UUID uuidUsuarioActivo);

}
