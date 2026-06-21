package com.llosa.backend.proyecto.entity;

import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.seguridad.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "usuario_activo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "uuidUsuarioActivo")
public class UsuarioActivo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_usuario_activo")
    private UUID uuidUsuarioActivo;

    @Column(name = "tipo_financiamiento")
    private String tipoFinanciamiento; // 'Crédito Directo', 'Crédito Hipotecario'

    @Column(name = "fecha_adquisicion")
    private LocalDateTime fechaAdquisicion;

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "vigente")
    private Boolean vigente = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Fundamental para auditoría comercial

    // =========================================================================
    // RELACIONES REFACTORIZADAS
    // =========================================================================

    /**
     * Múltiples propietarios/compradores (esposos, socios, hermanos).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_activo_clientes",
            joinColumns = @JoinColumn(name = "uuid_usuario_activo"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario")
    )
    @Builder.Default
    private List<Usuario> clientes = new ArrayList<>();

    /**
     * ¡REFACTORIZADO!: Múltiples bienes en un solo contrato (1 Dpto + 2 Cocheras).
     */
    @OneToMany(mappedBy = "usuarioActivo", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Activo> activos = new ArrayList<>();


    /**
     * Las fases que este contrato tiene que atravesar obligatoriamente.
     */
    @OneToMany(mappedBy = "usuarioActivo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EtapaExpediente> etapas = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_asesor")
    private Usuario asesor;

}