package com.llosa.backend.documentos.dto;

import com.llosa.backend.documentos.enums.TipoDocumento;
import jakarta.validation.constraints.NotNull;

public record SubirDocumentoRequest(

        @NotNull
        TipoDocumento tipoDocumento
) {}