package com.llosa.backend.factory;

import com.llosa.backend.proyecto.entity.Hito;

import com.llosa.backend.proyecto.enums.EstadoHito;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class FlujoConstruccionFactory {

    /**
     * Genera la lista de hitos de construcción por defecto para un nuevo Proyecto.
     * Esto se debe llamar desde el ProyectoService al momento de crear un proyecto.
     */
    public List<Hito> generarHitosPorDefecto() {
        List<Hito> hitos = new ArrayList<>();

        // --- LICENCIAS PREVIAS ---
        hitos.add(construirHito("Anteproyecto Aprobado", 1));
        hitos.add(construirHito("Licencia de Construcción", 2));

        // --- ETAPAS DE CONSTRUCCIÓN ---
        hitos.add(construirHito("Demolición", 3));
        hitos.add(construirHito("Inicio de obra", 4));
        hitos.add(construirHito("Excavación", 5 ));
        hitos.add(construirHito("Cimentación", 6));
        hitos.add(construirHito("Casco", 7 ));

        // Según tu documentación, los acabados pueden ir en paralelo, pero 
        // a nivel de tracker necesitan un orden secuencial visual.
        hitos.add(construirHito("Acabados húmedos", 8));
        hitos.add(construirHito("Acabados secos", 9));
        hitos.add(construirHito("Inmueble terminado", 10));

        return hitos;
    }

    /**
     * Helper para construir la entidad Hito de forma limpia.
     */
    private Hito construirHito(String titulo, int orden) {
        return Hito.builder()
                .titulo(titulo)
                .orden(orden)
                .estado(EstadoHito.PENDIENTE)
                .build();
    }
}