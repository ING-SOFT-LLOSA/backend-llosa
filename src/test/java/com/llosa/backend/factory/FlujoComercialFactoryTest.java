package com.llosa.backend.factory;

import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class FlujoComercialFactoryTest {

    private final FlujoComercialFactory factory = new FlujoComercialFactory();

    @Test
    void generarEtapasPorDefecto_creditoDirecto_retorna5Etapas() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);

        assertThat(etapas).hasSize(5);
        assertThat(etapas.get(0).getEtapaProceso()).isEqualTo(EtapaProceso.SEPARACION);
        assertThat(etapas.get(1).getEtapaProceso()).isEqualTo(EtapaProceso.CONTRATO);
        assertThat(etapas.get(2).getEtapaProceso()).isEqualTo(EtapaProceso.PAGO);
        assertThat(etapas.get(3).getEtapaProceso()).isEqualTo(EtapaProceso.ENTREGA);
        assertThat(etapas.get(4).getEtapaProceso()).isEqualTo(EtapaProceso.SANEAMIENTO);
    }

    @Test
    void generarEtapasPorDefecto_creditoHipotecario_retorna5Etapas() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Hipotecario")
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);

        assertThat(etapas).hasSize(5);
    }

    @Test
    void generarEtapasPorDefecto_separacion_tieneHitosYRequisitos() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);
        var separacion = etapas.get(0);

        assertThat(separacion.getHitosComerciales()).hasSize(4);
        assertThat(separacion.getRequisitos()).hasSize(3);
        assertThat(separacion.getHitosComerciales().get(0).getNombreHito()).isEqualTo("Proforma");
    }

    @Test
    void generarEtapasPorDefecto_contrato_tieneHitosYRequisitos() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);
        var contratoEtapa = etapas.get(1);

        assertThat(contratoEtapa.getHitosComerciales()).hasSize(5);
        assertThat(contratoEtapa.getRequisitos()).hasSize(4);
    }

    @Test
    void generarEtapasPorDefecto_pagoCreditoDirecto_tiene4Hitos() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);
        var pago = etapas.get(2);

        assertThat(pago.getHitosComerciales()).hasSize(4);
        assertThat(pago.getHitosComerciales().get(0).getNombreHito()).isEqualTo("Pago de Separación");
        assertThat(pago.getRequisitos()).isEmpty();
    }

    @Test
    void generarEtapasPorDefecto_pagoHipotecario_tiene6HitosY4Requisitos() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Hipotecario")
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);
        var pago = etapas.get(2);

        assertThat(pago.getHitosComerciales()).hasSize(6);
        assertThat(pago.getRequisitos()).hasSize(0);
    }

    @Test
    void generarEtapasPorDefecto_pago_pagoSeparacionPendiente() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);
        var pago = etapas.get(2);

        assertThat(pago.getHitosComerciales().get(0).getEstado()).isEqualTo(EstadoHitoComercial.PENDIENTE);
        assertThat(pago.getHitosComerciales().get(0).getFechaCompletado()).isNull();
    }

    @Test
    void generarEtapasPorDefecto_entrega_tieneHitosYRequisitos() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);
        var entrega = etapas.get(3);

        assertThat(entrega.getHitosComerciales()).hasSize(5);
        assertThat(entrega.getRequisitos()).hasSize(11);
    }

    @Test
    void generarEtapasPorDefecto_saneamiento_tieneHitosYRequisitos() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento("Credito Directo")
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);
        var saneamiento = etapas.get(4);

        assertThat(saneamiento.getHitosComerciales()).hasSize(7);
        assertThat(saneamiento.getRequisitos()).hasSize(4);
    }

    @Test
    void generarEtapasPorDefecto_tipoFinanciamientoNull_usaVacio() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento(null)
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);

        assertThat(etapas).hasSize(5);
        var pago = etapas.get(2);
        assertThat(pago.getHitosComerciales()).hasSize(4);
    }

    @Test
    void generarEtapasPorDefecto_financiamientoContieneHIPOT_detectaMayusculas() {
        var contrato = UsuarioActivo.builder()
                .tipoFinanciamiento("credito hipotecario")
                .build();

        var etapas = factory.generarEtapasPorDefecto(contrato);

        var pago = etapas.get(2);
        assertThat(pago.getHitosComerciales()).hasSize(6);
    }
}
