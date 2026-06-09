package com.llosa.backend.documentos.entity;

import com.llosa.backend.documentos.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_documento_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "tipoDocumento")
public class TipoDocumentoConfig {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 50)
    private TipoDocumento tipoDocumento;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    @Column(name = "mime_permitidos", nullable = false, length = 255)
    private String mimePermitidos;

    @Column(name = "max_size_bytes", nullable = false)
    private Long maxSizeBytes;
}