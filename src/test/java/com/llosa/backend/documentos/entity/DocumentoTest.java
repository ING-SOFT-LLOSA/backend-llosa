package com.llosa.backend.documentos.entity;

import com.llosa.backend.documentos.enums.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentoTest {

    @Test
    void testGettersAndSetters() {
        Documento doc = new Documento();
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        doc.setId(id);
        doc.setNombreOriginal("test.pdf");
        doc.setRutaGcs("ruta/gcs");
        doc.setTipoDocumento(TipoDocumento.PDF_LEGAL);
        doc.setTipoMime("application/pdf");
        doc.setIdReferencia("ref1");
        doc.setEntidadReferencia("PROYECTO");
        doc.setSubidoPor(1);
        doc.setAccesoRestringido(true);
        doc.setCreatedAt(now);

        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getNombreOriginal()).isEqualTo("test.pdf");
        assertThat(doc.getRutaGcs()).isEqualTo("ruta/gcs");
        assertThat(doc.getTipoDocumento()).isEqualTo(TipoDocumento.PDF_LEGAL);
        assertThat(doc.getTipoMime()).isEqualTo("application/pdf");
        assertThat(doc.getIdReferencia()).isEqualTo("ref1");
        assertThat(doc.getEntidadReferencia()).isEqualTo("PROYECTO");
        assertThat(doc.getSubidoPor()).isEqualTo(1);
    }

    @Test
    void testBuilder() {
        UUID id = UUID.randomUUID();
        Documento doc = Documento.builder()
                .id(id)
                .nombreOriginal("test.pdf")
                .build();

        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getNombreOriginal()).isEqualTo("test.pdf");
    }
}
