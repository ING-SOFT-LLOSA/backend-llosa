package com.llosa.backend.factory;

import com.llosa.backend.proyecto.enums.EstadoHito;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FlujoConstruccionFactoryTest {

    private final FlujoConstruccionFactory factory = new FlujoConstruccionFactory();

    @Test
    void generarHitosPorDefecto_retorna10Hitos() {
        var hitos = factory.generarHitosPorDefecto();

        assertThat(hitos).hasSize(10);
    }

    @Test
    void generarHitosPorDefecto_ordenCorrecto() {
        var hitos = factory.generarHitosPorDefecto();

        assertThat(hitos.get(0).getTitulo()).isEqualTo("Anteproyecto Aprobado");
        assertThat(hitos.get(0).getOrden()).isEqualTo(1);
        assertThat(hitos.get(1).getTitulo()).isEqualTo("Licencia de Construcción");
        assertThat(hitos.get(1).getOrden()).isEqualTo(2);
        assertThat(hitos.get(9).getTitulo()).isEqualTo("Inmueble terminado");
        assertThat(hitos.get(9).getOrden()).isEqualTo(10);
    }

    @Test
    void generarHitosPorDefecto_todosPendientes() {
        var hitos = factory.generarHitosPorDefecto();

        assertThat(hitos).allMatch(h -> h.getEstado() == EstadoHito.PENDIENTE);
    }
}
