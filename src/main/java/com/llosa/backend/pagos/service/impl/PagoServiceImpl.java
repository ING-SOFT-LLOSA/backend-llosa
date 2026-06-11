package com.llosa.backend.pagos.service.impl;

import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.pagos.dto.PagoRequest;
import com.llosa.backend.pagos.dto.PagoResponse;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.pagos.entity.Pago;
import com.llosa.backend.pagos.repository.CronogramaPagoRepository;
import com.llosa.backend.pagos.repository.PagoRepository;
import com.llosa.backend.pagos.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final CronogramaPagoRepository cronogramaPagoRepository;
    private final DocumentoService documentoService;

    @Override
    public List<PagoResponse> listarPorCronograma(UUID uuidCronograma) {
        return pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(uuidCronograma)
                .stream()
                .map(PagoResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public PagoResponse agregarCuota(UUID uuidCronograma, PagoRequest request) {
        CronogramaPago cp = cronogramaPagoRepository.findById(uuidCronograma)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cronograma no encontrado: " + uuidCronograma));

        if (pagoRepository.findByCronograma_IdAndNroCuota(uuidCronograma, request.nroCuota()).isPresent()) {
            throw new BusinessException("Ya existe una cuota con el número " + request.nroCuota());
        }

        Pago pago = Pago.builder()
                .cronograma(cp)
                .nroCuota(request.nroCuota())
                .montoProgramado(request.montoProgramado())
                .fechaVencimiento(request.fechaVencimiento())
                .estado("PENDIENTE")
                .build();

        Pago guardado = pagoRepository.save(pago);
        log.info("Cuota {} agregada al cronograma {}", request.nroCuota(), uuidCronograma);
        return PagoResponse.fromEntity(guardado);
    }

    @Override
    @Transactional
    public PagoResponse actualizarCuota(UUID uuidPago, PagoRequest request) {
        Pago pago = pagoRepository.findById(uuidPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado: " + uuidPago));

        if (!pago.getNroCuota().equals(request.nroCuota())
                && pagoRepository.findByCronograma_IdAndNroCuota(
                        pago.getCronograma().getId(), request.nroCuota()).isPresent()) {
            throw new BusinessException("Ya existe una cuota con el número " + request.nroCuota());
        }

        pago.setNroCuota(request.nroCuota());
        pago.setMontoProgramado(request.montoProgramado());
        pago.setFechaVencimiento(request.fechaVencimiento());

        Pago guardado = pagoRepository.save(pago);
        log.info("Cuota actualizada: {}", uuidPago);
        return PagoResponse.fromEntity(guardado);
    }

    @Override
    @Transactional
    public void eliminarCuota(UUID uuidPago) {
        if (!pagoRepository.existsById(uuidPago)) {
            throw new RecursoNoEncontradoException("Pago no encontrado: " + uuidPago);
        }
        pagoRepository.deleteById(uuidPago);
        log.info("Cuota eliminada: {}", uuidPago);
    }

    @Override
    @Transactional
    public PagoResponse cambiarEstado(UUID uuidPago, String nuevoEstado, Integer actualizadoPor) {
        Pago pago = pagoRepository.findById(uuidPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado: " + uuidPago));

        if (!List.of("PENDIENTE", "PAGADO", "VENCIDO").contains(nuevoEstado)) {
            throw new BusinessException("Estado inválido: " + nuevoEstado);
        }

        pago.setEstado(nuevoEstado);

        if ("PAGADO".equals(nuevoEstado)) {
            pago.setFechaPago(LocalDateTime.now());
            if (pago.getMontoPagado().compareTo(java.math.BigDecimal.ZERO) == 0) {
                pago.setMontoPagado(pago.getMontoProgramado());
            }
        } else if ("VENCIDO".equals(nuevoEstado)) {
            pago.setFechaPago(null);
        }

        pago.setActualizadoPor(actualizadoPor);

        Pago guardado = pagoRepository.save(pago);
        log.info("Estado de cuota {} actualizado a: {}", uuidPago, nuevoEstado);
        return PagoResponse.fromEntity(guardado);
    }

    @Override
    @Transactional
    public PagoResponse subirComprobante(UUID uuidPago, MultipartFile file, Integer subidoPor) {
        Pago pago = pagoRepository.findById(uuidPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado: " + uuidPago));

        DocumentoResponse doc = documentoService.subirDocumentoPolimorfico(
                pago.getCronograma().getUsuarioActivo().getUuidUsuarioActivo(),
                file,
                TipoDocumento.COMPROBANTE,
                uuidPago.toString(),
                "PAGO",
                subidoPor
        );

        pago.setUuidComprobante(doc.id());
        if (!"PAGADO".equals(pago.getEstado())) {
            pago.setEstado("PAGADO");
            pago.setFechaPago(LocalDateTime.now());
            if (pago.getMontoPagado().compareTo(java.math.BigDecimal.ZERO) == 0) {
                pago.setMontoPagado(pago.getMontoProgramado());
            }
        }
        pago.setActualizadoPor(subidoPor);

        Pago guardado = pagoRepository.save(pago);
        log.info("Comprobante subido para cuota {}: {}", uuidPago, doc.id());
        return PagoResponse.fromEntity(guardado);
    }
}
