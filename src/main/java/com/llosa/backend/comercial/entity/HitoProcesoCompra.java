package com.llosa.backend.comercial.entity;

import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que representa un hito dentro del proceso de compra comercial.
 * Cada hito pertenece a una etapa del proceso y está asociado a un UsuarioActivo (expediente).
 */
@Entity
@Table(name = "hito_proceso_compra")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "uuidHitoComercial")
@ToString(exclude = "usuarioActivo")
public class HitoProcesoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_hito_comercial", updatable = false, nullable = false)
    private UUID uuidHitoComercial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_usuario_activo", nullable = false)
    private UsuarioActivo usuarioActivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_proceso", nullable = false)
    private EtapaProceso etapaProceso;

    @Column(name = "nombre_hito", nullable = false)
    private String nombreHito;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "estado", nullable = false)
    private EstadoHitoComercial estado = EstadoHitoComercial.PENDIENTE;

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
