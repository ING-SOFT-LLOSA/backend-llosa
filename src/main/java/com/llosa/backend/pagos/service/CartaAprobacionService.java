package com.llosa.backend.pagos.service;

import com.llosa.backend.pagos.dto.CartaAprobacionRequest;
import com.llosa.backend.pagos.dto.CartaAprobacionResponse;

import java.util.UUID;

public interface CartaAprobacionService {

    CartaAprobacionResponse crear(CartaAprobacionRequest request);

    CartaAprobacionResponse obtenerPorUsuarioActivo(UUID uuidUsuarioActivo);

    CartaAprobacionResponse actualizar(UUID uuidCarta, CartaAprobacionRequest request);

    void eliminar(UUID uuidCarta);
}
