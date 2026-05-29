package com.llosa.backend.proyecto.service.impl;

import com.llosa.backend.proyecto.dto.response.SeguimientoResponseDTO;
import com.llosa.backend.proyecto.dto.shared.FaseActualDTO;
import com.llosa.backend.proyecto.dto.shared.PasoStepperDTO;
import com.llosa.backend.proyecto.entity.HitoUnidad;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.repository.HitoUnidadRepository;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.proyecto.service.SeguimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeguimientoServiceImpl implements SeguimientoService {

    private final UsuarioActivoRepository usuarioActivoRepository;
    private final HitoUnidadRepository hitoUnidadRepository;

    public SeguimientoResponseDTO obtenerSeguimiento(UUID idActivo) {

        Optional<UsuarioActivo> expedienteOpt = usuarioActivoRepository.findByActivoId(idActivo);
        List<HitoUnidad> hitos = hitoUnidadRepository.findByActivo_IdOrderByHito_OrdenAsc(idActivo);

        List<PasoStepperDTO> stepper = new ArrayList<>();

        // Maquina de estado
        // --- EVALUACIÓN DE ESTADOS ---
        String estadoSeparacion = expedienteOpt.isPresent() ? "COMPLETADO" : "PENDIENTE";
        stepper.add(new PasoStepperDTO("Separación", estadoSeparacion, 1));

        // PASO 2: Contrato (Lógica Comercial)
        String estadoContrato = "PENDIENTE";
        if (estadoSeparacion.equals("COMPLETADO")) {
            String tramite = expedienteOpt.get().getEstadoTramiteLegal();
            boolean contratoFirmado = tramite != null && tramite.equalsIgnoreCase("EscrituraFirmada");
            estadoContrato = contratoFirmado ? "COMPLETADO" : "ACTIVO";
        }
        stepper.add(new PasoStepperDTO("Contrato", estadoContrato, 2));

        String estadoConstruccion = "PENDIENTE";
        if (!hitos.isEmpty()) {
            boolean todosHitosCompletados = hitos.stream()
                    .allMatch(h -> h.getEstado().name().equals("COMPLETADO"));

            estadoConstruccion = todosHitosCompletados ? "COMPLETADO" : "ACTIVO";
        }
        stepper.add(new PasoStepperDTO("Construcción", estadoConstruccion, 3));

        String estadoEntrega = "PENDIENTE";
        if (estadoConstruccion.equals("COMPLETADO") && expedienteOpt.isPresent()) {
            String tramite = expedienteOpt.get().getEstadoTramiteLegal();
            boolean entregado = tramite != null && tramite.equalsIgnoreCase("ActaDeEntrega");
            estadoEntrega = entregado ? "COMPLETADO" : "ACTIVO";
        }
        stepper.add(new PasoStepperDTO("Entrega", estadoEntrega, 4));

        // PASO 5: Saneamiento (Requiere Entrega)
        String estadoSaneamiento = "PENDIENTE";
        if (estadoEntrega.equals("COMPLETADO") && expedienteOpt.isPresent()) {
            String tramite = expedienteOpt.get().getEstadoTramiteLegal();
            boolean independizado = tramite != null && tramite.equalsIgnoreCase("PartidaIndependizadoSUNARP");
            estadoSaneamiento = independizado ? "COMPLETADO" : "ACTIVO";
        }
        stepper.add(new PasoStepperDTO("Saneamiento", estadoSaneamiento, 5));
        return new SeguimientoResponseDTO(
                stepper,
                calcularFaseActual(hitos)
        );
    }

    private FaseActualDTO calcularFaseActual(List<HitoUnidad> hitos) {
        if (hitos == null || hitos.isEmpty()) {
            return new FaseActualDTO(
                    null,
                    "Pendiente de inicio",
                    "La etapa de construcción aún no ha comenzado.",
                    0,
                    LocalDateTime.now()
            );
        }

        int totalHitos = hitos.size();
        int hitosCompletados = 0;
        HitoUnidad hitoActivo = null;

        for (HitoUnidad hu : hitos) {
            if (hu.getEstado().name().equals("COMPLETADO")) {
                hitosCompletados++;
            } else if (hitoActivo == null) {
                hitoActivo = hu;
            }
        }

        if (hitoActivo == null) {
            hitoActivo = hitos.getLast();
        }

        double porcentajeEtapa = (((double) hitosCompletados / totalHitos) * 100);

        String titulo = hitoActivo.getHito() != null ? hitoActivo.getHito().getTitulo(): "Fase de Obra";

        String descripcion = (hitoActivo.getObservaciones() != null && !hitoActivo.getObservaciones().isBlank())
                ? hitoActivo.getObservaciones()
                : "La construcción avanza según los plazos establecidos.";
        return new FaseActualDTO(
                hitoActivo.getId(),           // El UUID real de la tabla HitoUnidad
                titulo,                       // El nombre real del hito de catálogo
                descripcion,                  // La observación real del ingeniero
                porcentajeEtapa,              // El porcentaje matemáticamente exacto
                hitoActivo.getUpdatedAt() != null ? hitoActivo.getUpdatedAt() : hitoActivo.getCreatedAt()
        );
    }

}
