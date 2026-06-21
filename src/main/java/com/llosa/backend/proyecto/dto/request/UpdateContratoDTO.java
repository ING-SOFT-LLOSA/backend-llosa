package com.llosa.backend.proyecto.dto.request;
import java.time.LocalDateTime;
import java.util.List;

public record UpdateContratoDTO(
        List<Integer> idsUsuarios,
        String tipoFinanciamiento,
        LocalDateTime fechaAdquisicion,
        LocalDateTime fechaCompletado
) {}