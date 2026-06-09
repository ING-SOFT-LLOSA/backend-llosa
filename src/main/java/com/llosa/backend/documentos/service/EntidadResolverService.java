package com.llosa.backend.documentos.service;

import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.proyecto.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntidadResolverService {

    private final ProyectoRepository proyectoRepository;
    private final ActivoRepository activoRepository;
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final HitoRepository hitoRepository;
    private final HitoPisoRepository hitoPisoRepository;
    private final ReporteRepository reporteRepository;

    /**
     * Recibe un UUID y determina a qué entidad del paquete proyecto pertenece.
     * Torre y Piso usan Long como ID, por lo que no aplican aquí.
     */
    public String resolverEntidad(UUID id) {
        if (proyectoRepository.existsById(id))      return "PROYECTO";
        if (activoRepository.existsById(id))         return "ACTIVO";
        if (usuarioActivoRepository.existsById(id))  return "USUARIO_ACTIVO";
        if (hitoRepository.existsById(id))           return "HITO";
        if (hitoPisoRepository.existsById(id))       return "HITO_PISO";
        if (reporteRepository.existsById(id))        return "REPORTE";

        throw new BusinessException(
                "El UUID '" + id + "' no corresponde a ninguna entidad del módulo de proyectos.");
    }
}