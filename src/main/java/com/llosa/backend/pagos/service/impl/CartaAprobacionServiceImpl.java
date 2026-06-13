package com.llosa.backend.pagos.service.impl;

import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.EtapaExpedienteRepository;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.pagos.dto.CartaAprobacionRequest;
import com.llosa.backend.pagos.dto.CartaAprobacionResponse;
import com.llosa.backend.pagos.entity.CartaAprobacion;
import com.llosa.backend.pagos.repository.CartaAprobacionRepository;
import com.llosa.backend.pagos.service.CartaAprobacionService;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CartaAprobacionServiceImpl implements CartaAprobacionService {

    private final CartaAprobacionRepository cartaAprobacionRepository;
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final EtapaExpedienteRepository etapaExpedienteRepository;

    @Override
    @Transactional
    public CartaAprobacionResponse crear(CartaAprobacionRequest request) {
        if (cartaAprobacionRepository.existsByUsuarioActivo_UuidUsuarioActivo(request.uuidUsuarioActivo())) {
            throw new BusinessException("El expediente ya tiene una carta de aprobación registrada");
        }

        UsuarioActivo ua = usuarioActivoRepository.findById(request.uuidUsuarioActivo())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Expediente no encontrado: " + request.uuidUsuarioActivo()));

        CartaAprobacion carta = CartaAprobacion.builder()
                .usuarioActivo(ua)
                .banco(request.banco())
                .montoAprobado(request.montoAprobado())
                .fechaEmision(request.fechaEmision())
                .fechaVencimiento(request.fechaVencimiento())
                .fechaDesembolsoProyectada(request.fechaDesembolsoProyectada())
                .comentarios(request.comentarios())
                .build();

        CartaAprobacion guardada = cartaAprobacionRepository.save(carta);
        log.info("Carta de aprobación creada: {} para expediente: {}", guardada.getId(), request.uuidUsuarioActivo());


        return CartaAprobacionResponse.fromEntity(guardada);
    }

    @Override
    public CartaAprobacionResponse obtenerPorUsuarioActivo(UUID uuidUsuarioActivo) {
        CartaAprobacion ca = cartaAprobacionRepository
                .findByUsuarioActivo_UuidUsuarioActivo(uuidUsuarioActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay carta de aprobación para el expediente: " + uuidUsuarioActivo));
        return CartaAprobacionResponse.fromEntity(ca);
    }

    @Override
    @Transactional
    public CartaAprobacionResponse actualizar(UUID uuidCarta, CartaAprobacionRequest request) {
        CartaAprobacion ca = cartaAprobacionRepository.findById(uuidCarta)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Carta de aprobación no encontrada: " + uuidCarta));

        ca.setBanco(request.banco());
        ca.setMontoAprobado(request.montoAprobado());
        ca.setFechaEmision(request.fechaEmision());
        ca.setFechaVencimiento(request.fechaVencimiento());
        ca.setFechaDesembolsoProyectada(request.fechaDesembolsoProyectada());
        ca.setComentarios(request.comentarios());

        CartaAprobacion guardada = cartaAprobacionRepository.save(ca);
        log.info("Carta de aprobación actualizada: {}", uuidCarta);
        return CartaAprobacionResponse.fromEntity(guardada);
    }

    @Override
    @Transactional
    public void eliminar(UUID uuidCarta) {
        if (!cartaAprobacionRepository.existsById(uuidCarta)) {
            throw new RecursoNoEncontradoException("Carta de aprobación no encontrada: " + uuidCarta);
        }
        cartaAprobacionRepository.deleteById(uuidCarta);
        log.info("Carta de aprobación eliminada: {}", uuidCarta);
    }
}
