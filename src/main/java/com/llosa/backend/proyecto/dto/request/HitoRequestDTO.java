package com.llosa.backend.proyecto.dto.request;


import com.llosa.backend.proyecto.enums.EstadoHito;

import java.time.LocalDate;

public record HitoRequestDTO(
        String titulo,
        String nombre,
        String descripcion,
        EstadoHito estado,
        LocalDate fechaEstimada
) {}
