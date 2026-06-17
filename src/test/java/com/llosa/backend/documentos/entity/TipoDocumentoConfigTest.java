package com.llosa.backend.documentos.entity;

import com.llosa.backend.documentos.enums.TipoDocumento;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TipoDocumentoConfigTest {

    @Test
    void testGettersAndSetters() {
        TipoDocumentoConfig config = new TipoDocumentoConfig();
        
        config.setTipoDocumento(TipoDocumento.PDF_LEGAL);
        config.setMaxSizeBytes(1024L);
        config.setMimePermitidos("application/pdf");

        assertThat(config.getTipoDocumento()).isEqualTo(TipoDocumento.PDF_LEGAL);
        assertThat(config.getMaxSizeBytes()).isEqualTo(1024L);
        assertThat(config.getMimePermitidos()).isEqualTo("application/pdf");
    }
}
