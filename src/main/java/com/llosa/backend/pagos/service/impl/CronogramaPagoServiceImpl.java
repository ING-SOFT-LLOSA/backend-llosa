package com.llosa.backend.pagos.service.impl;

import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.exception.EntidadDuplicadaException;
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
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
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
    
    static private final String CRONOGRAMA_NO_ENCONTRADO_MSG = "Cronograma no encontrado: ";
    
    @Override
    @Transactional
    public CronogramaPagoResponse crear(CronogramaPagoRequest request) {
        if (cronogramaPagoRepository.existsByUsuarioActivo_UuidUsuarioActivo(request.uuidUsuarioActivo())) {
            throw new EntidadDuplicadaException("El expediente ya tiene un cronograma de pagos activo");
        }

        UsuarioActivo ua = usuarioActivoRepository.findById(request.uuidUsuarioActivo())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Expediente no encontrado: " + request.uuidUsuarioActivo()));

        CronogramaPago cronograma = CronogramaPago.builder()
                .usuarioActivo(ua)
                .totalPactado(request.totalPactado())
                .numeroCuotas(request.numeroCuotas())
                .pagoInicial(request.pagoIncial() != null ? request.pagoIncial() : BigDecimal.ZERO)
                .pagoSeparacion(request.pagoSeparacion() != null ? request.pagoSeparacion() : BigDecimal.ZERO)
                .estado("ACTIVO")
                .build();

        CronogramaPago guardado = cronogramaPagoRepository.save(cronograma);

        // Generar pagos automáticos según tipo de financiamiento
        List<Pago> pagos = pagoFlujoFactory.generarPagos(guardado, ua.getTipoFinanciamiento());
        // Correcion
        vincularPagosARequisitos(pagos, ua.getUuidUsuarioActivo());

        pagoRepository.saveAll(pagos);

        log.info("Cronograma creado: {} para expediente: {} con {} pagos generados",
                guardado.getId(), request.uuidUsuarioActivo(), pagos.size());

        return CronogramaPagoResponse.fromEntity(guardado);
    }

    private void vincularPagosARequisitos(List<Pago> pagos, UUID uuidUsuarioActivo) {
        for (Pago pago : pagos) {
            switch (pago.getConcepto()) {
                case SEPARACION -> buscarRequisito(uuidUsuarioActivo, EtapaProceso.SEPARACION, "Comprobante de separación")
                        .ifPresent(pago::setUuidRequisitoDocumental);

                case INICIAL -> buscarRequisito(uuidUsuarioActivo, EtapaProceso.CONTRATO, "Pago Inicial")
                        .ifPresent(pago::setUuidRequisitoDocumental);

                default -> {
                    // No requiere vinculación
                }
            }
        }
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
                        CRONOGRAMA_NO_ENCONTRADO_MSG + uuidCronograma));

        cp.setTotalPactado(request.totalPactado());
        cp.setNumeroCuotas(request.numeroCuotas());
        cp.setPagoInicial(request.pagoIncial() != null ? request.pagoIncial() : BigDecimal.ZERO);
        cp.setPagoSeparacion(request.pagoSeparacion() != null ? request.pagoSeparacion() : BigDecimal.ZERO);

        CronogramaPago guardado = cronogramaPagoRepository.save(cp);
        log.info("Cronograma actualizado: {}", uuidCronograma);
        return CronogramaPagoResponse.fromEntity(guardado);
    }

    @Override
    @Transactional
    public void eliminar(UUID uuidCronograma) {
        if (!cronogramaPagoRepository.existsById(uuidCronograma)) {
            throw new RecursoNoEncontradoException(CRONOGRAMA_NO_ENCONTRADO_MSG + uuidCronograma);
        }
        cronogramaPagoRepository.deleteById(uuidCronograma);
        log.info("Cronograma eliminado: {}", uuidCronograma);
    }

    @Override
    @Transactional
    public ResumenResponseHipotecarioDTO obtenerResumenHipotecario(UUID uuidCronograma) {
        CronogramaPago cp = cronogramaPagoRepository.findById(uuidCronograma)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        CRONOGRAMA_NO_ENCONTRADO_MSG + uuidCronograma));

        BigDecimal montoTotal = cp.getTotalPactado();
        BigDecimal pagoSeparacion = cp.getPagoSeparacion() != null ? cp.getPagoSeparacion() : BigDecimal.ZERO;
        BigDecimal pagoInicial = cp.getPagoInicial() != null ? cp.getPagoInicial() : BigDecimal.ZERO;
        BigDecimal totalPagado = pagoSeparacion.add(pagoInicial);
        BigDecimal saldoPendiente = montoTotal.subtract(totalPagado);

        EstadoGlobalPago estadoGlobal;
        if (pagoSeparacion.signum() > 0 && pagoInicial.signum() > 0) {
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
                        CRONOGRAMA_NO_ENCONTRADO_MSG + uuidCronograma));

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
}
