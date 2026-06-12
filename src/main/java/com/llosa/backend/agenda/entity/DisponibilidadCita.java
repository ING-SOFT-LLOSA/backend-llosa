package com.llosa.backend.agenda.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Representa un bloque horario propuesto por el cliente para reprogramar
 * una cita (funcionalidad tipo When2meet).
 *
 * El flujo es:
 * 1. El gestor habilita permiteReprogramacion = true en la cita.
 * 2. El cliente propone uno o más bloques mediante POST /api/agenda/citas/{id}/disponibilidad.
 * 3. El gestor revisa, elige el bloque final y actualiza la cita.
 * 4. La fecha en Google Calendar solo cambia cuando el gestor confirma desde el Portal Empresa.
 */
@Entity
@Table(name = "disponibilidad_cita")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DisponibilidadCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_cita", nullable = false)
    private Cita cita;

    @Column(name = "bloque_inicio", nullable = false)
    private LocalDateTime bloqueInicio;

    @Column(name = "bloque_fin", nullable = false)
    private LocalDateTime bloqueFin;

    /**
     * true = este bloque fue seleccionado por el gestor como el definitivo.
     */
    @Column(name = "seleccionado", nullable = false)
    @Builder.Default
    private Boolean seleccionado = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
