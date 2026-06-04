package com.llosa.backend.comercial.service;

import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.dto.HitoComercialRequest;
import com.llosa.backend.comercial.dto.HitoComercialResponse;
import com.llosa.backend.comercial.dto.StepperResponse;

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
