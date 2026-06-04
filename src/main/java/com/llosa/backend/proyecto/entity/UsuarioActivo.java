package com.llosa.backend.proyecto.entity;

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

    @Column(name = "fase_comercial")
    private String faseComercial; // 'Separación', 'Contrato', 'Pagos', etc.

    @Column(name = "estado_tramite_legal")
    private String estadoTramiteLegal; // 'Minuta Pendiente', 'Escritura Firmada', 'Partida registral SUNARP'

    @Column(name = "fecha_adquisicion")
    private LocalDateTime fechaAdquisicion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Fundamental para auditoría comercial

    // RELACIONES CON OTRAS TABLAS

    /**
     * Lista de copropietarios del proceso comercial.
     * Un proceso puede pertenecer a múltiples clientes (ej: esposos, socios).
     * La tabla intermedia "usuario_activo_clientes" gestiona la relación N:M.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_activo_clientes",
            joinColumns = @JoinColumn(name = "uuid_usuario_activo"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario")
    )
    @Builder.Default
    private List<Usuario> clientes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid_activo", nullable = false)
    private Activo activo;
}