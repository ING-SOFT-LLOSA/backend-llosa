package com.llosa.backend.comercial.entity;

import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "etapa_expediente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "uuidEtapaExpediente")
@ToString(exclude = {"usuarioActivo", "hitosComerciales", "requisitos"})
public class EtapaExpediente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_etapa_expediente", updatable = false, nullable = false)
    private UUID uuidEtapaExpediente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uuid_usuario_activo", nullable = false)
    private UsuarioActivo usuarioActivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_proceso", nullable = false)
    private EtapaProceso etapaProceso; // CONTRATO, MINUTA, etc.

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "PENDIENTE"; // PENDIENTE, EN_CURSO, COMPLETADA

    // Una etapa contiene sus propios hitos secuenciales de negocio
    @OneToMany(mappedBy = "etapaExpediente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HitoProcesoCompra> hitosComerciales = new ArrayList<>();

    // Una etapa contiene el checklist de documentos requeridos
    @OneToMany(mappedBy = "etapaExpediente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RequisitoDocumental> requisitos = new ArrayList<>();
}