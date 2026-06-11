package com.llosa.backend.comercial.entity;

import com.llosa.backend.comercial.enums.EtapaRequisitoDocumental;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "requisito_documental")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "etapaExpediente")
public class RequisitoDocumental {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_requisito_documental", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uuid_etapa_expediente", nullable = false)
    private EtapaExpediente etapaExpediente;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "nota_corporativa", columnDefinition = "TEXT")
    private String notaCorporativa;

    @Column(name = "estado", nullable = false, length = 30)
    private EtapaRequisitoDocumental estado;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "icono", length = 100)
    private String icono;
}
