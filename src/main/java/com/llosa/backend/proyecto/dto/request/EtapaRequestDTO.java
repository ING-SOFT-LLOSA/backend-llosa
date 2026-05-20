package com.llosa.backend.proyecto.dto.request;


import com.llosa.backend.proyecto.enums.EstadoEtapa;

public record EtapaRequestDTO(
        String nombre,
        String descripcion,
        EstadoEtapa estado
) {}
