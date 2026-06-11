package com.llosa.backend.agenda.entity;

import com.llosa.backend.agenda.enums.EstadoCita;
import com.llosa.backend.agenda.enums.EstadoSincronizacion;
import com.llosa.backend.agenda.enums.TipoEvento;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.seguridad.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa una cita o evento agendado entre un gestor interno y un cliente,
 * vinculada a una unidad inmobiliaria específica.
 *
 * Soporta dos modos de sincronización:
 * - Google Calendar: cuando el cliente usa cuenta Google, el evento se sincroniza
 *   via Google Calendar API y se guarda el googleEventId.
 * - Solo Backend: cuando el cliente no tiene Google, la cita se persiste aquí
 *   y el frontend la consulta directamente mediante el endpoint /api/agenda/cliente.
 */
@Entity
@Table(name = "cita")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_cita", updatable = false, nullable = false)
    private UUID id;

    /** Empleado (Postventa/Asesor) que organiza la cita como anfitrión. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_gestor", nullable = false)
    private Usuario gestor;

    /** Cliente al que se invita a la cita. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Usuario cliente;

    /** Unidad inmobiliaria objeto de la reunión. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_activo", nullable = false)
    private Activo activo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 50)
    private TipoEvento tipoEvento;

    @Column(name = "titulo", length = 200)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "ubicacion", length = 300)
    private String ubicacion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cita", nullable = false, length = 30)
    @Builder.Default
    private EstadoCita estadoCita = EstadoCita.PROGRAMADA;

    /**
     * Indica si el cliente confirmó la asistencia:
     * null = sin respuesta, true = confirmado, false = declinado.
     */
    @Column(name = "confirmacion_cliente")
    private Boolean confirmacionCliente;

    /**
     * true → el cliente puede proponer una reprogramación via When2meet.
     * false → la cita es fija (ej. Junta de Propietarios, inauguración).
     */
    @Column(name = "permite_reprogramacion", nullable = false)
    @Builder.Default
    private Boolean permiteReprogramacion = false;

    // ---- Integración Google Calendar ----

    /** ID del evento en Google Calendar. Null si el usuario no tiene Google. */
    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_sincronizacion", nullable = false, length = 20)
    @Builder.Default
    private EstadoSincronizacion estadoSincronizacion = EstadoSincronizacion.PENDIENTE;

    /**
     * Indica si el cliente tiene cuenta Google y puede usar Calendar.
     * false → la cita solo vive en el backend y el front la consulta directamente.
     */
    @Column(name = "cliente_usa_google", nullable = false)
    @Builder.Default
    private Boolean clienteUsaGoogle = false;

    // ---- Auditoría ----

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Motivo registrado cuando se cancela o resuelve la cita. */
    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    /** Bloques de disponibilidad propuestos por el cliente (When2meet). */
    @OneToMany(mappedBy = "cita", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DisponibilidadCita> disponibilidades = new ArrayList<>();
}
