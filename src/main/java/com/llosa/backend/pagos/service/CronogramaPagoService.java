package com.llosa.backend.pagos.service;

import com.llosa.backend.pagos.dto.CronogramaPagoRequest;
import com.llosa.backend.pagos.dto.CronogramaPagoResponse;
import com.llosa.backend.pagos.dto.ResumenResponse;

import java.util.UUID;

public interface CronogramaPagoService {

    CronogramaPagoResponse crear(CronogramaPagoRequest request);

    CronogramaPagoResponse obtenerPorUsuarioActivo(UUID uuidUsuarioActivo);

    CronogramaPagoResponse actualizar(UUID uuidCronograma, CronogramaPagoRequest request);

    void eliminar(UUID uuidCronograma);

    ResumenResponse obtenerResumen(UUID uuidCronograma);
}
