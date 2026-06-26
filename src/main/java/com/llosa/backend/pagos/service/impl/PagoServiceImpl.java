package com.llosa.backend.pagos.service.impl;

import com.llosa.backend.agenda.dto.request.ActualizarCitaRequest;
import com.llosa.backend.agenda.service.AgendaService;
import com.llosa.backend.comercial.service.RequisitoDocumentalService;
import com.llosa.backend.documentos.dto.DocumentoResponse;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.documentos.service.DocumentoService;
import com.llosa.backend.exception.EntidadDuplicadaException;
import com.llosa.backend.pagos.ConceptoPago;
import com.llosa.backend.exception.EstadoInvalidoException;
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
    private final DocumentoRepository documentoRepository;
    private final RequisitoDocumentalService requisitoDocumentalService;
    private final AgendaService agendaService;

    private static final String PAGO_NO_ENCONTRADO = "Pago no encontrado: ";
    private static final String PAGADO = "PAGADO";

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

        // Validar que no exista duplicado por concepto único (SEPARACION, INICIAL, DESEMBOLSO_COMPLETADO)
        if (request.concepto() != null && request.concepto() != ConceptoPago.CUOTA
                && pagoRepository.findByCronograma_IdAndConcepto(uuidCronograma, request.concepto()).isPresent()) {
            throw new EntidadDuplicadaException(
                    "Ya existe un pago de tipo " + request.concepto() + " para este cronograma. Edítalo en vez de crear otro.");
        }

        // Validar duplicado por nroCuota (solo aplica para CUOTA)
        if (pagoRepository.findByCronograma_IdAndNroCuota(uuidCronograma, request.nroCuota()).isPresent()) {
            throw new EntidadDuplicadaException("Ya existe una cuota con el número " + request.nroCuota());
        }

        Pago pago = Pago.builder()
                .cronograma(cp)
                .nroCuota(request.nroCuota())
                .montoProgramado(request.montoProgramado())
                .fechaVencimiento(request.fechaVencimiento())
                .estado("PENDIENTE")
                .concepto(request.concepto())
                .comentario(request.comentario())
                .build();

        Pago guardado = pagoRepository.save(pago);
        log.info("Cuota {} agregada al cronograma {}", request.nroCuota(), uuidCronograma);
        return PagoResponse.fromEntity(guardado);
    }

    @Override
    @Transactional
    public PagoResponse actualizarCuota(UUID uuidPago, PagoRequest request) {
        Pago pago = pagoRepository.findById(uuidPago)
                .orElseThrow(() -> new RecursoNoEncontradoException(PAGO_NO_ENCONTRADO + uuidPago));

        // Validar que no exista duplicado por concepto único (solo si cambia de concepto)
        if (request.concepto() != null && request.concepto() != pago.getConcepto()
                && request.concepto() != ConceptoPago.CUOTA
                && pagoRepository.findByCronograma_IdAndConcepto(
                        pago.getCronograma().getId(), request.concepto()).isPresent()) {
            throw new EntidadDuplicadaException(
                    "Ya existe un pago de tipo " + request.concepto() + " para este cronograma. Edítalo en vez de crear otro.");
        }

        if (!pago.getNroCuota().equals(request.nroCuota())
                && pagoRepository.findByCronograma_IdAndNroCuota(
                        pago.getCronograma().getId(), request.nroCuota()).isPresent()) {
            throw new EntidadDuplicadaException("Ya existe una cuota con el número " + request.nroCuota());
        }

        LocalDate oldVencimiento = pago.getFechaVencimiento();

        pago.setNroCuota(request.nroCuota());
        pago.setMontoProgramado(request.montoProgramado());
        pago.setFechaVencimiento(request.fechaVencimiento());
        if (request.concepto() != null) {
            pago.setConcepto(request.concepto());
        }
        if (request.comentario() != null) {
            pago.setComentario(request.comentario());
        }

        Pago guardado = pagoRepository.save(pago);

        // Sincronizar CronogramaPago si se actualizó un pago de concepto fijo
        if (guardado.getConcepto() == ConceptoPago.SEPARACION) {
            CronogramaPago cp = guardado.getCronograma();
            cp.setPagoSeparacion(guardado.getMontoProgramado());
            cronogramaPagoRepository.save(cp);
        } else if (guardado.getConcepto() == ConceptoPago.INICIAL) {
            CronogramaPago cp = guardado.getCronograma();
            cp.setPagoInicial(guardado.getMontoProgramado());
            cronogramaPagoRepository.save(cp);
        }

        // Actualizar fecha de la cita vinculada si cambió la fecha de vencimiento
        if (guardado.getUuidCita() != null && !guardado.getFechaVencimiento().equals(oldVencimiento)) {
            agendaService.actualizarCita(guardado.getUuidCita(),
                    new ActualizarCitaRequest(null, null, null,
                            guardado.getFechaVencimiento().atTime(10, 0),
                            guardado.getFechaVencimiento().atTime(11, 0),
                            null, null, null));
        }

        log.info("Cuota actualizada: {}", uuidPago);
        return PagoResponse.fromEntity(guardado);
    }

    @Override
    @Transactional
    public void eliminarCuota(UUID uuidPago) {
        Pago pago = pagoRepository.findById(uuidPago)
                .orElseThrow(() -> new RecursoNoEncontradoException(PAGO_NO_ENCONTRADO + uuidPago));

        UUID uuidCita = pago.getUuidCita();
        pagoRepository.deleteById(uuidPago);

        if (uuidCita != null) {
            try {
                agendaService.cancelarCita(uuidCita, "Cuota eliminada del cronograma");
            } catch (Exception e) {
                log.warn("[Pago] No se pudo cancelar la cita {} vinculada al pago {}: {}",
                        uuidCita, uuidPago, e.getMessage());
            }
        }

        log.info("Cuota eliminada: {}", uuidPago);
    }

    @Override
    @Transactional
    public PagoResponse cambiarEstado(UUID uuidPago, String nuevoEstado, Integer actualizadoPor) {
        Pago pago = pagoRepository.findById(uuidPago)
                .orElseThrow(() -> new RecursoNoEncontradoException(PAGO_NO_ENCONTRADO + uuidPago));

        if (!List.of("PENDIENTE", PAGADO, "VENCIDO").contains(nuevoEstado)) {
            throw new EstadoInvalidoException("Estado inválido: " + nuevoEstado);
        }

        pago.setEstado(nuevoEstado);

        if (PAGADO.equals(nuevoEstado)) {
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
    public PagoResponse subirComprobante(UUID uuidPago, MultipartFile file, Integer subidoPor, String comentario) {
        Pago pago = pagoRepository.findById(uuidPago)
                .orElseThrow(() -> new RecursoNoEncontradoException(PAGO_NO_ENCONTRADO + uuidPago));

        DocumentoResponse doc = documentoService.subirDocumentoPolimorfico(
                file,
                TipoDocumento.COMPROBANTE,
                uuidPago.toString(),
                "PAGO",
                subidoPor
        );

        pago.setUuidComprobante(doc.id());

        // Si el pago está vinculado a un requisito documental, completarlo también
        if (pago.getUuidRequisitoDocumental() != null) {
            Documento docEntity = documentoRepository.findById(doc.id())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado: " + doc.id()));
            requisitoDocumentalService.completarRequisitoConDocumento(
                    pago.getUuidRequisitoDocumental(),
                    docEntity.getRutaGcs(),
                    docEntity.getNombreOriginal(),
                    docEntity.getTipoMime(),
                    subidoPor,
                    comentario != null ? comentario : pago.getComentario()
            );
        }

        // Guardar comentario
        if (comentario != null) {
            pago.setComentario(comentario);
        }

        if (!PAGADO.equals(pago.getEstado())) {
            pago.setEstado(PAGADO);
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
