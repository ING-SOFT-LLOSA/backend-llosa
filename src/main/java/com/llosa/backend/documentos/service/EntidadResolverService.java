package com.llosa.backend.documentos.service;

import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.exception.BusinessException;
import com.llosa.backend.pagos.repository.CronogramaPagoRepository;
import com.llosa.backend.pagos.repository.PagoRepository;
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
    private final RequisitoDocumentalRepository requisitoDocumentalRepository;
    private final PagoRepository pagoRepository;
    private final CronogramaPagoRepository cronogramaPagoRepository;

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
        if (requisitoDocumentalRepository.existsById(id)) {return "REQUISITO_DOCUMENTAL";}
        if (cronogramaPagoRepository.existsById(id)) return "CRONOGRAMA_PAGO";
        if (pagoRepository.existsById(id))           return "PAGO";

        throw new BusinessException(
                "El UUID '" + id + "' no corresponde a ninguna entidad del módulo de proyectos.");
    }
}