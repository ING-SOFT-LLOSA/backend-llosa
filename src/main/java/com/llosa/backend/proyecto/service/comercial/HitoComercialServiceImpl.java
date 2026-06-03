package com.llosa.backend.proyecto.service.comercial;

import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.proyecto.dto.comercial.*;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.entity.comercial.EstadoHitoComercial;
import com.llosa.backend.proyecto.entity.comercial.EtapaProceso;
import com.llosa.backend.proyecto.entity.comercial.HitoProcesoCompra;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.proyecto.repository.comercial.HitoProcesoCompraRepository;
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

    @Override
    public HitoComercialResponse crearHito(HitoComercialRequest request) {
        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(request.getUuidUsuarioActivo())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "UsuarioActivo no encontrado con UUID: " + request.getUuidUsuarioActivo()));

        HitoProcesoCompra hito = HitoProcesoCompra.builder()
                .usuarioActivo(usuarioActivo)
                .etapaProceso(request.getEtapaProceso())
                .nombreHito(request.getNombreHito())
                .descripcion(request.getDescripcion())
                .orden(request.getOrden())
                .estado(EstadoHitoComercial.PENDIENTE)
                .build();

        HitoProcesoCompra guardado = hitoRepository.save(hito);
        log.info("Hito comercial creado: {} para UsuarioActivo: {}",
                guardado.getUuidHitoComercial(), request.getUuidUsuarioActivo());

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
    public HitoComercialResponse actualizarEstado(UUID uuidHitoComercial, EstadoHitoComercial nuevoEstado) {
        HitoProcesoCompra hito = hitoRepository.findById(uuidHitoComercial)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Hito comercial no encontrado con UUID: " + uuidHitoComercial));

        hito.setEstado(nuevoEstado);

        // Lógica de negocio: marcar/limpiar fecha de completado automáticamente
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
                    List<HitoProcesoCompra> hitosDeEtapa =
                            hitosPorEtapa.getOrDefault(etapa, Collections.emptyList());

                    List<HitoComercialResponse> hitosResponse = hitosDeEtapa.stream()
                            .map(HitoComercialResponse::fromEntity)
                            .toList();

                    double porcentaje = calcularPorcentajeAvance(hitosDeEtapa);

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

    @Override
    public StepperResponse inicializarHitosPorDefecto(UUID uuidUsuarioActivo) {
        UsuarioActivo usuarioActivo = usuarioActivoRepository.findById(uuidUsuarioActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "UsuarioActivo no encontrado con UUID: " + uuidUsuarioActivo));

        // Prevenir doble inicialización
        if (hitoRepository.existsByUsuarioActivo_UuidUsuarioActivo(uuidUsuarioActivo)) {
            throw new BusinessException(
                    "Los hitos ya fueron inicializados para el UsuarioActivo: " + uuidUsuarioActivo);
        }

        List<HitoProcesoCompra> hitosDefecto = generarHitosPorDefecto(usuarioActivo);
        hitoRepository.saveAll(hitosDefecto);
        log.info("Hitos por defecto inicializados para UsuarioActivo: {}", uuidUsuarioActivo);

        return obtenerStepper(uuidUsuarioActivo);
    }

    // ======================== MÉTODOS PRIVADOS ========================

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

    /**
     * Genera el set de hitos básicos por defecto para las 5 etapas del proceso de compra.
     */
    private List<HitoProcesoCompra> generarHitosPorDefecto(UsuarioActivo usuarioActivo) {
        List<HitoProcesoCompra> hitos = new ArrayList<>();

        // SEPARACIÓN
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.SEPARACION, "Subir voucher de separación", "Adjuntar comprobante de pago de separación", 1));
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.SEPARACION, "Validar voucher", "Confirmar la recepción del pago de separación", 2));
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.SEPARACION, "Generar carta de separación", "Emitir documento oficial de separación del inmueble", 3));

        // CONTRATO
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.CONTRATO, "Firma de Minuta", "Firmar la minuta de compra-venta", 1));
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.CONTRATO, "Escritura Pública", "Elevar la minuta a escritura pública ante notario", 2));
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.CONTRATO, "Inscripción en SUNARP", "Registrar la propiedad en la SUNARP", 3));

        // PAGOS
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.PAGOS, "Pago de cuota inicial", "Registrar el pago de la cuota inicial", 1));
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.PAGOS, "Aprobación de crédito", "Confirmar aprobación del crédito hipotecario/directo", 2));
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.PAGOS, "Desembolso bancario", "Verificar el desembolso del banco al promotor", 3));

        // ENTREGA
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.ENTREGA, "Programar fecha de entrega", "Coordinar la fecha de entrega del inmueble", 1));
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.ENTREGA, "Inspección pre-entrega", "Realizar la inspección del inmueble antes de la entrega", 2));
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.ENTREGA, "Acta de entrega firmada", "Firmar el acta de conformidad de entrega", 3));

        // SANEAMIENTO
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.SANEAMIENTO, "Declaratoria de fábrica", "Tramitar la declaratoria de fábrica del inmueble", 1));
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.SANEAMIENTO, "Independización", "Gestionar la independización de la partida registral", 2));
        hitos.add(buildHitoDefecto(usuarioActivo, EtapaProceso.SANEAMIENTO, "Partida registral individual", "Obtener la partida registral individual del inmueble", 3));

        return hitos;
    }

    /**
     * Builder helper para construir un hito por defecto.
     */
    private HitoProcesoCompra buildHitoDefecto(UsuarioActivo usuarioActivo, EtapaProceso etapa,
                                                 String nombre, String descripcion, int orden) {
        return HitoProcesoCompra.builder()
                .usuarioActivo(usuarioActivo)
                .etapaProceso(etapa)
                .nombreHito(nombre)
                .descripcion(descripcion)
                .orden(orden)
                .estado(EstadoHitoComercial.PENDIENTE)
                .build();
    }
}
