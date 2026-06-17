package com.llosa.backend.documentos.entity;

import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.proyecto.entity.Proyecto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documento", indexes = {
        @Index(name = "idx_documento_polimorfico", columnList = "entidad_referencia, id_referencia")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid_documento", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ruta_gcs", nullable = false, length = 500)
    private String rutaGcs;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "id_referencia", nullable = false, length = 36)
    private String idReferencia;

    @Column(name = "entidad_referencia", nullable = false, length = 50)
    private String entidadReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 50)
    private TipoDocumento tipoDocumento;

    @Column(name = "tipo_mime", length = 50)
    private String tipoMime;

    // FIX: Added @Builder.Default here because you initialized it with "= true"
    @Builder.Default
    @Column(name = "acceso_restringido", nullable = false)
    private boolean accesoRestringido = true;

    @Column(name = "subido_por")
    private Integer subidoPor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}