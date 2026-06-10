package com.llosa.backend.pagos.service;

import com.llosa.backend.pagos.dto.PagoRequest;
import com.llosa.backend.pagos.dto.PagoResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface PagoService {

    List<PagoResponse> listarPorCronograma(UUID uuidCronograma);

    PagoResponse agregarCuota(UUID uuidCronograma, PagoRequest request);

    PagoResponse actualizarCuota(UUID uuidPago, PagoRequest request);

    void eliminarCuota(UUID uuidPago);

    PagoResponse cambiarEstado(UUID uuidPago, String nuevoEstado, Integer actualizadoPor);

    PagoResponse subirComprobante(UUID uuidPago, MultipartFile file, Integer subidoPor);
}
