package com.llosa.backend.agenda.dto.response;

import com.llosa.backend.agenda.entity.Cita;
import com.llosa.backend.agenda.enums.EstadoCita;
import com.llosa.backend.agenda.enums.EstadoSincronizacion;
import com.llosa.backend.agenda.enums.TipoEvento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CitaResponse(
        UUID id,
        TipoEvento tipoEvento,
        String titulo,
        String descripcion,
        String ubicacion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        EstadoCita estadoCita,
        Boolean confirmacionCliente,
        Boolean permiteReprogramacion,
        Boolean clienteUsaGoogle,
        EstadoSincronizacion estadoSincronizacion,
        String googleEventId,

        // Info resumida del cliente
        Integer clienteId,
        String clienteNombre,
        String clienteEmail,

        // Info resumida del gestor
        Integer gestorId,
        String gestorNombre,

        // Info de la unidad
        UUID activoId,
        String activoNro,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        List<DisponibilidadResponse> disponibilidades
) {
    public static CitaResponse fromEntity(Cita c) {
        return new CitaResponse(
                c.getId(),
                c.getTipoEvento(),
                c.getTitulo(),
                c.getDescripcion(),
                c.getUbicacion(),
                c.getFechaInicio(),
                c.getFechaFin(),
                c.getEstadoCita(),
                c.getConfirmacionCliente(),
                c.getPermiteReprogramacion(),
                c.getClienteUsaGoogle(),
                c.getEstadoSincronizacion(),
                c.getGoogleEventId(),
                c.getCliente().getId(),
                c.getCliente().getNombre() + " " + c.getCliente().getApellidos(),
                c.getCliente().getEmail(),
                c.getGestor().getId(),
                c.getGestor().getNombre() + " " + c.getGestor().getApellidos(),
                c.getActivo().getId(),
                c.getActivo().getNro(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getDisponibilidades().stream()
                        .map(DisponibilidadResponse::fromEntity)
                        .toList()
        );
    }
}
