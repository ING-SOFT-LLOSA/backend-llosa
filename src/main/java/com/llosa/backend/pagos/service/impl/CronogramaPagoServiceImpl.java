package com.llosa.backend.pagos.service.impl;

import com.llosa.backend.agenda.dto.request.CrearCitaRequest;
import com.llosa.backend.agenda.dto.response.CitaResponse;
import com.llosa.backend.agenda.enums.TipoEvento;
import com.llosa.backend.agenda.service.AgendaService;
import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.EntidadDuplicadaException;
import com.llosa.backend.exception.EstadoInvalidoException;
import com.llosa.backend.pagos.ConceptoPago;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.factory.PagoFlujoFactory;
import com.llosa.backend.pagos.EstadoGlobalPago;
import com.llosa.backend.pagos.dto.CronogramaPagoRequest;
import com.llosa.backend.pagos.dto.CronogramaPagoResponse;
import com.llosa.backend.pagos.dto.ResumenResponse;
import com.llosa.backend.pagos.dto.ResumenResponseHipotecarioDTO;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.pagos.entity.Pago;
import com.llosa.backend.pagos.repository.CronogramaPagoRepository;
import com.llosa.backend.pagos.repository.PagoRepository;
import com.llosa.backend.pagos.service.CronogramaPagoService;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.seguridad.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CronogramaPagoServiceImpl implements CronogramaPagoService {

    private final CronogramaPagoRepository cronogramaPagoRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final PagoFlujoFactory pagoFlujoFactory;
    private final EtapaExpedienteRepository etapaExpedienteRepository;
    private final RequisitoDocumentalRepository requisitoDocumentalRepository;
    private final AgendaService agendaService;

    @Override
    @Transactional
    public CronogramaPagoResponse crear(CronogramaPagoRequest request) {
        if (cronogramaPagoRepository.existsByUsuarioActivo_UuidUsuarioActivo(request.uuidUsuarioActivo())) {
            throw new EntidadDuplicadaException("El expediente ya tiene un cronograma de pagos activo");
        }

        UsuarioActivo ua = usuarioActivoRepository.findById(request.uuidUsuarioActivo())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Expediente no encontrado: " + request.uuidUsuarioActivo()));

        validarConsistenciaMontos(request);

        CronogramaPago cronograma = CronogramaPago.builder()
                .usuarioActivo(ua)
                .totalPactado(request.totalPactado())
                .numeroCuotas(request.numeroCuotas())
                .pagoInicial(request.pagoInicial() != null ? request.pagoInicial() : BigDecimal.ZERO)
                .pagoSeparacion(request.pagoSeparacion() != null ? request.pagoSeparacion() : BigDecimal.ZERO)
                .estado(CronogramaPago.ESTADO_ACTIVO)
                .build();

        CronogramaPago guardado = cronogramaPagoRepository.save(cronograma);

        // Generar pagos automáticos según tipo de financiamiento
        List<Pago> pagos = pagoFlujoFactory.generarPagos(guardado, ua.getTipoFinanciamiento());

        // Vincular pagos de separación e inicial a sus requisitos documentales
        for (Pago pago : pagos) {
            switch (pago.getConcepto()) {
                case SEPARACION -> {
                    Optional<UUID> reqId = buscarRequisito(ua.getUuidUsuarioActivo(),
                            EtapaProceso.SEPARACION, "Comprobante de separación");
                    reqId.ifPresent(pago::setUuidRequisitoDocumental);
                }
                case INICIAL -> {
                    Optional<UUID> reqId = buscarRequisito(ua.getUuidUsuarioActivo(),
                            EtapaProceso.CONTRATO, "Pago Inicial");
                    reqId.ifPresent(pago::setUuidRequisitoDocumental);
                }
                default -> { }
            }
        }

        List<Pago> pagosGuardados = pagoRepository.saveAll(pagos);

        // Crear recordatorios de calendario para cuotas futuras (solo si hay asesor)
        Usuario gestor = ua.getAsesor();
        if (gestor != null) {
            Usuario cliente = ua.getClientes().isEmpty() ? null : ua.getClientes().get(0);
            Activo activo = ua.getActivos().isEmpty() ? null : ua.getActivos().get(0);

            for (Pago pago : pagosGuardados) {
                if (pago.getConcepto() != ConceptoPago.CUOTA) continue;
                if (pago.getFechaVencimiento().isBefore(LocalDate.now())) continue;
                if (cliente == null || activo == null) {
                    log.warn("[Cronograma] No se pudo crear recordatorio de pago para cuota {}: faltan cliente o activo",
                            pago.getNroCuota());
                    continue;
                }

                CrearCitaRequest citaReq = new CrearCitaRequest(
                        cliente.getId(),
                        activo.getId(),
                        TipoEvento.RECORDATORIO_PAGO,
                        "Vencimiento de cuota N° " + pago.getNroCuota(),
                        null,
                        null,
                        pago.getFechaVencimiento().atTime(10, 0),
                        pago.getFechaVencimiento().atTime(11, 0),
                        false,
                        false
                );

                CitaResponse citaResponse = agendaService.crearCita(gestor.getFirebaseUuid(), citaReq);
                pago.setUuidCita(citaResponse.id());
            }
        } else {
            log.info("[Cronograma] Expediente {} sin asesor asignado, no se crean recordatorios de pago",
                    ua.getUuidUsuarioActivo());
        }

        pagoRepository.saveAll(pagosGuardados);
        log.info("Cronograma creado: {} para expediente: {} con {} pagos generados",
                guardado.getId(), request.uuidUsuarioActivo(), pagos.size());
        return CronogramaPagoResponse.fromEntity(guardado);
    }

    private Optional<UUID> buscarRequisito(UUID uuidUsuarioActivo, EtapaProceso etapa, String titulo) {
        return etapaExpedienteRepository
                .findByUsuarioActivo_UuidUsuarioActivoAndEtapaProceso(uuidUsuarioActivo, etapa)
                .flatMap(etapaEx -> requisitoDocumentalRepository
                        .findByEtapaExpediente_UuidEtapaExpedienteAndTitulo(
                                etapaEx.getUuidEtapaExpediente(), titulo)
                        .map(RequisitoDocumental::getId));
    }

    @Override
    public CronogramaPagoResponse obtenerPorUsuarioActivo(UUID uuidUsuarioActivo) {
        CronogramaPago cp = cronogramaPagoRepository
                .findByUsuarioActivo_UuidUsuarioActivo(uuidUsuarioActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay cronograma para el expediente: " + uuidUsuarioActivo));
        return CronogramaPagoResponse.fromEntity(cp);
    }

    @Override
    @Transactional
    public CronogramaPagoResponse actualizar(UUID uuidCronograma, CronogramaPagoRequest request) {
        CronogramaPago cp = cronogramaPagoRepository.findById(uuidCronograma)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cronograma no encontrado: " + uuidCronograma));

        if (CronogramaPago.ESTADO_HISTORICO.equals(cp.getEstado())) {
            throw new EstadoInvalidoException(
                    "No se puede modificar un cronograma en estado HISTORICO");
        }

        validarConsistenciaMontos(request);

        cp.setTotalPactado(request.totalPactado());
        cp.setNumeroCuotas(request.numeroCuotas());
        cp.setPagoInicial(request.pagoInicial() != null ? request.pagoInicial() : BigDecimal.ZERO);
        cp.setPagoSeparacion(request.pagoSeparacion() != null ? request.pagoSeparacion() : BigDecimal.ZERO);

        CronogramaPago guardado = cronogramaPagoRepository.save(cp);

        // Sincronizar Pagos existentes con los nuevos montos del cronograma
        pagoRepository.findByCronograma_IdAndConcepto(uuidCronograma, ConceptoPago.SEPARACION)
                .ifPresent(p -> { p.setMontoProgramado(guardado.getPagoSeparacion()); pagoRepository.save(p); });
        pagoRepository.findByCronograma_IdAndConcepto(uuidCronograma, ConceptoPago.INICIAL)
                .ifPresent(p -> { p.setMontoProgramado(guardado.getPagoInicial()); pagoRepository.save(p); });

        log.info("Cronograma actualizado: {}", uuidCronograma);
        return CronogramaPagoResponse.fromEntity(guardado);
    }

    @Override
    @Transactional
    public void eliminar(UUID uuidCronograma) {
        CronogramaPago cp = cronogramaPagoRepository.findById(uuidCronograma)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cronograma no encontrado: " + uuidCronograma));

        if (CronogramaPago.ESTADO_HISTORICO.equals(cp.getEstado())) {
            throw new EstadoInvalidoException(
                    "No se puede eliminar un cronograma en estado HISTORICO");
        }

        cronogramaPagoRepository.deleteById(uuidCronograma);
        log.info("Cronograma eliminado: {}", uuidCronograma);
    }

    @Override
    @Transactional
    public ResumenResponseHipotecarioDTO obtenerResumenHipotecario(UUID uuidCronograma) {
        CronogramaPago cp = cronogramaPagoRepository.findById(uuidCronograma)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cronograma no encontrado: " + uuidCronograma));

        BigDecimal montoTotal = cp.getTotalPactado();

        BigDecimal montoSeparacionPagado = pagoRepository
                .findByCronograma_IdAndConcepto(uuidCronograma, ConceptoPago.SEPARACION)
                .filter(p -> "PAGADO".equals(p.getEstado()))
                .map(p -> p.getMontoPagado() != null ? p.getMontoPagado() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);

        BigDecimal montoInicialPagado = pagoRepository
                .findByCronograma_IdAndConcepto(uuidCronograma, ConceptoPago.INICIAL)
                .filter(p -> "PAGADO".equals(p.getEstado()))
                .map(p -> p.getMontoPagado() != null ? p.getMontoPagado() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalPagado = montoSeparacionPagado.add(montoInicialPagado);
        BigDecimal saldoPendiente = montoTotal.subtract(totalPagado);

        EstadoGlobalPago estadoGlobal;
        if (montoSeparacionPagado.compareTo(BigDecimal.ZERO) > 0
                && montoInicialPagado.compareTo(BigDecimal.ZERO) > 0) {
            estadoGlobal = EstadoGlobalPago.AL_DIA;
        } else {
            estadoGlobal = EstadoGlobalPago.RETRASADO;
        }

        return new ResumenResponseHipotecarioDTO(
                montoTotal,
                totalPagado,
                saldoPendiente,
                estadoGlobal
        );
    }

    @Override
    public ResumenResponse obtenerResumen(UUID uuidCronograma) {
        CronogramaPago cp = cronogramaPagoRepository.findById(uuidCronograma)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cronograma no encontrado: " + uuidCronograma));

        List<Pago> pagos = pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(uuidCronograma);

        BigDecimal totalPagado = BigDecimal.ZERO;
        long cuotasPagadas = 0;
        long cuotasPendientes = 0;
        long cuotasVencidas = 0;
        LocalDate proximoVencimiento = null;

        for (Pago p : pagos) {
            switch (p.getEstado()) {
                case "PAGADO" -> {
                    totalPagado = totalPagado.add(p.getMontoPagado());
                    cuotasPagadas++;
                }
                case "VENCIDO" -> {
                    cuotasVencidas++;
                    totalPagado = totalPagado.add(p.getMontoPagado());
                }
                default -> {
                    cuotasPendientes++;
                    if (p.getEstado().equals("PENDIENTE")
                            && (proximoVencimiento == null || p.getFechaVencimiento().isBefore(proximoVencimiento))) {
                        if (!p.getFechaVencimiento().isBefore(LocalDate.now())) {
                            proximoVencimiento = p.getFechaVencimiento();
                        }
                    }
                }
            }
        }

        BigDecimal totalPendiente = cp.getTotalPactado().subtract(totalPagado);
        String estadoGlobal = calcularEstadoGlobal(cp, cuotasVencidas, cuotasPagadas, pagos.size());

        return new ResumenResponse(
                cp.getTotalPactado(),
                totalPagado,
                totalPendiente,
                estadoGlobal,
                cuotasPagadas,
                cuotasPendientes,
                cuotasVencidas,
                proximoVencimiento
        );
    }

    private String calcularEstadoGlobal(CronogramaPago cp, long cuotasVencidas, long cuotasPagadas, int totalCuotas) {
        if (totalCuotas == 0) return "ACTIVO";
        if (cuotasPagadas == totalCuotas) return "LIQUIDADO";
        if (cuotasVencidas > 0) return "EN_MORA";
        if (hayCuotasProximasAVencer(cp)) return "EN_RIESGO";
        return "AL_DIA";
    }

    private boolean hayCuotasProximasAVencer(CronogramaPago cp) {
        List<Pago> pagos = pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(cp.getId());
        LocalDate hoy = LocalDate.now();
        for (Pago p : pagos) {
            if ("PENDIENTE".equals(p.getEstado())
                    && !p.getFechaVencimiento().isBefore(hoy)
                    && p.getFechaVencimiento().isBefore(hoy.plusDays(7))) {
                return true;
            }
        }
        return false;
    }

    private void validarConsistenciaMontos(CronogramaPagoRequest request) {
        BigDecimal separacion = request.pagoSeparacion() != null ? request.pagoSeparacion() : BigDecimal.ZERO;
        BigDecimal inicial = request.pagoInicial() != null ? request.pagoInicial() : BigDecimal.ZERO;
        if (separacion.add(inicial).compareTo(request.totalPactado()) > 0) {
            throw new BusinessException(
                    "La suma del pago de separación e inicial no puede exceder el total pactado");
        }
    }
}
