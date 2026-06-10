package com.llosa.backend.comercial.service.impl;

import com.llosa.backend.comercial.dto.EtapaStepperResponse;
import com.llosa.backend.comercial.dto.HitoComercialRequest;
import com.llosa.backend.comercial.dto.HitoComercialResponse;
import com.llosa.backend.comercial.dto.StepperResponse;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.comercial.service.HitoComercialService;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.pagos.entity.CronogramaPago;
import com.llosa.backend.pagos.entity.Pago;
import com.llosa.backend.pagos.repository.CronogramaPagoRepository;
import com.llosa.backend.pagos.repository.PagoRepository;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de Hitos Comerciales (Proceso de Compra).
 * Contiene toda la lógica de negocio para gestionar los hitos del flujo comercial.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HitoComercialServiceImpl implements HitoComercialService {

    private final HitoProcesoCompraRepository hitoRepository;
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final CronogramaPagoRepository cronogramaPagoRepository;
    private final PagoRepository pagoRepository;

    @Override
    public HitoComercialResponse crearHito(HitoComercialRequest request) {
        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(request.uuidUsuarioActivo())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "UsuarioActivo no encontrado con UUID: " + request.uuidUsuarioActivo()));

        HitoProcesoCompra hito = HitoProcesoCompra.builder()
                .usuarioActivo(usuarioActivo)
                .etapaProceso(request.etapaProceso())
                .nombreHito(request.nombreHito())
                .descripcion(request.descripcion())
                .orden(request.orden())
                .estado(EstadoHitoComercial.PENDIENTE)
                .createdAt(LocalDateTime.now())
                .build();

        HitoProcesoCompra guardado = hitoRepository.save(hito);
        log.info("Hito comercial creado: {} para UsuarioActivo: {}",
                guardado.getUuidHitoComercial(), request.uuidUsuarioActivo());

        return HitoComercialResponse.fromEntity(guardado);
    }

    @Override
    public void eliminarHito(UUID uuidHitoComercial) {
        if (!hitoRepository.existsById(uuidHitoComercial)) {
            throw new RecursoNoEncontradoException(
                    "Hito comercial no encontrado con UUID: " + uuidHitoComercial);
        }
        hitoRepository.deleteById(uuidHitoComercial);
        log.info("Hito comercial eliminado: {}", uuidHitoComercial);
    }

    @Override
    public HitoComercialResponse actualizarEstado(
            UUID uuidHitoComercial,
            EstadoHitoComercial nuevoEstado) {

        HitoProcesoCompra hito = hitoRepository.findById(uuidHitoComercial)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Hito comercial no encontrado con UUID: " + uuidHitoComercial));

        if (nuevoEstado == EstadoHitoComercial.COMPLETADO) {

            Integer ordenActual = hito.getOrden();

            if (ordenActual > 1) {

                HitoProcesoCompra hitoAnterior = hitoRepository
                        .findByUsuarioActivo_UuidUsuarioActivoAndOrden(
                                hito.getUsuarioActivo().getUuidUsuarioActivo(),
                                ordenActual - 1
                        )
                        .orElseThrow(() -> new IllegalStateException(
                                "No existe el hito anterior para el orden " + (ordenActual - 1)
                        ));

                if (hitoAnterior.getEstado() != EstadoHitoComercial.COMPLETADO) {
                    throw new IllegalStateException(
                            "Debe completar primero el hito anterior: " + hitoAnterior.getNombreHito()
                    );
                }
            }
        }

        hito.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoHitoComercial.COMPLETADO) {
            hito.setFechaCompletado(LocalDateTime.now());
        } else {
            hito.setFechaCompletado(null);
        }

        HitoProcesoCompra actualizado = hitoRepository.save(hito);

        log.info("Estado del hito {} actualizado a: {}", uuidHitoComercial, nuevoEstado);

        return HitoComercialResponse.fromEntity(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public StepperResponse obtenerStepper(UUID uuidUsuarioActivo) {
        // Validar que el UsuarioActivo existe
        if (!usuarioActivoRepository.existsById(uuidUsuarioActivo)) {
            throw new RecursoNoEncontradoException(
                    "UsuarioActivo no encontrado con UUID: " + uuidUsuarioActivo);
        }

        List<HitoProcesoCompra> todosLosHitos =
                hitoRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(uuidUsuarioActivo);

        // Agrupar hitos por etapa
        Map<EtapaProceso, List<HitoProcesoCompra>> hitosPorEtapa = todosLosHitos.stream()
                .collect(Collectors.groupingBy(HitoProcesoCompra::getEtapaProceso));

        // Construir la lista de etapas respetando el orden natural del enum
        List<EtapaStepperResponse> etapas = Arrays.stream(EtapaProceso.values())
                .map(etapa -> {
                    List<HitoComercialResponse> hitosResponse;

                    if (etapa == EtapaProceso.PAGO) {
                        hitosResponse = buildPagosHitos(uuidUsuarioActivo, hitosPorEtapa.getOrDefault(etapa, Collections.emptyList()));
                    } else {
                        List<HitoProcesoCompra> hitosDeEtapa =
                                hitosPorEtapa.getOrDefault(etapa, Collections.emptyList());
                        hitosResponse = hitosDeEtapa.stream()
                                .map(HitoComercialResponse::fromEntity)
                                .toList();
                    }

                    double porcentaje = calcularPorcentajeAvanceResponse(hitosResponse);

                    return EtapaStepperResponse.builder()
                            .etapa(etapa)
                            .hitos(hitosResponse)
                            .porcentajeAvance(porcentaje)
                            .build();
                })
                .toList();

        return StepperResponse.builder()
                .uuidUsuarioActivo(uuidUsuarioActivo)
                .etapas(etapas)
                .build();
    }

    // ======================== MÉTODOS PRIVADOS ========================

    /**
     * Para la etapa PAGO, si existe un CronogramaPago, construye los hitos
     * a partir de las cuotas reales (Pago) en lugar de los HitoProcesoCompra almacenados.
     */
    private List<HitoComercialResponse> buildPagosHitos(UUID uuidUsuarioActivo, List<HitoProcesoCompra> hitosAlmacenados) {
        Optional<CronogramaPago> optCp = cronogramaPagoRepository
                .findByUsuarioActivo_UuidUsuarioActivo(uuidUsuarioActivo);

        if (optCp.isEmpty()) {
            return hitosAlmacenados.stream()
                    .map(HitoComercialResponse::fromEntity)
                    .toList();
        }

        List<Pago> pagos = pagoRepository.findByCronograma_IdOrderByNroCuotaAsc(optCp.get().getId());

        if (pagos.isEmpty()) {
            return hitosAlmacenados.stream()
                    .map(HitoComercialResponse::fromEntity)
                    .toList();
        }

        return pagos.stream()
                .map(p -> {
                    EstadoHitoComercial estado = switch (p.getEstado()) {
                        case "PAGADO" -> EstadoHitoComercial.COMPLETADO;
                        case "VENCIDO" -> EstadoHitoComercial.COMPLETADO;
                        default -> EstadoHitoComercial.PENDIENTE;
                    };

                    String nombre = p.getNroCuota() == 0
                            ? "Cuota Inicial"
                            : "Cuota " + p.getNroCuota();

                    return HitoComercialResponse.builder()
                            .uuidHitoComercial(p.getId())
                            .uuidUsuarioActivo(uuidUsuarioActivo)
                            .etapaProceso(EtapaProceso.PAGO)
                            .nombreHito(nombre)
                            .descripcion("S/. " + p.getMontoProgramado()
                                    + " — Vence: " + p.getFechaVencimiento())
                            .orden(p.getNroCuota())
                            .estado(estado)
                            .fechaCompletado(p.getFechaPago())
                            .createdAt(p.getCreatedAt())
                            .build();
                })
                .toList();
    }

    /**
     * Calcula el porcentaje de avance a partir de una lista de HitoComercialResponse.
     */
    private double calcularPorcentajeAvanceResponse(List<HitoComercialResponse> hitos) {
        if (hitos.isEmpty()) return 0.0;
        long completados = hitos.stream()
                .filter(h -> h.estado() == EstadoHitoComercial.COMPLETADO)
                .count();
        return Math.round(((double) completados / hitos.size()) * 100.0 * 100.0) / 100.0;
    }

    /**
     * Calcula el porcentaje de avance de una lista de hitos.
     * Fórmula: (hitos COMPLETADOS / total hitos) * 100
     */
    private double calcularPorcentajeAvance(List<HitoProcesoCompra> hitos) {
        if (hitos.isEmpty()) {
            return 0.0;
        }
        long completados = hitos.stream()
                .filter(h -> h.getEstado() == EstadoHitoComercial.COMPLETADO)
                .count();
        return Math.round(((double) completados / hitos.size()) * 100.0 * 100.0) / 100.0;
    }

}
