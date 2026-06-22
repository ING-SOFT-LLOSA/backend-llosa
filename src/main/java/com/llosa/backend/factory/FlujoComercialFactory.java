package com.llosa.backend.factory;

import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EstadoEtapaExpediente;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.enums.EtapaRequisitoDocumental;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class FlujoComercialFactory {
    /**
     * Genera las etapas e hitos por defecto para un nuevo contrato.
     */

    private static final String PAGO_INCIAL = "Pago Inicial";
    private static final String NO_NOTE_CORPORATIVA = "No hay nota corporativa";

    public List<EtapaExpediente> generarEtapasPorDefecto(UsuarioActivo contrato) {

        List<EtapaExpediente> etapas = new ArrayList<>();

        String tipoFinanciamiento = contrato.getTipoFinanciamiento() == null
                ? ""
                : contrato.getTipoFinanciamiento().toUpperCase(Locale.ROOT);

        // 1. ETAPA: SEPARACIÓN
        EtapaExpediente separacion = construirEtapa(contrato, EtapaProceso.SEPARACION);
        separacion.getHitosComerciales().add(construirHito(separacion, "Proforma", 1,
                "Documento preliminar con precio, forma de pago y características de la unidad"));
        separacion.getHitosComerciales().add(construirHito(separacion, "Pago de separación", 2,
                "Comprobante del monto de separación de la unidad"));
        separacion.getHitosComerciales().add(construirHito(separacion, "Ficha del cliente", 3,
                "Registro base del expediente con datos del comprador y del bien adquirido"));
        separacion.getHitosComerciales().add(construirHito(separacion, "Separación", 4,
                "La unidad queda reservada a nombre del cliente"));

        separacion.getRequisitos().add(construirRequisito(
                separacion,
                "Proforma",
                "Documento que detalla las condiciones preliminares de la compra: precio, forma de pago y características de la unidad.",
                "request_quote",
                "Emitido por el departamento comercial. Las condiciones tienen una vigencia de 7 días calendario desde su emisión."
        ));

        separacion.getRequisitos().add(construirRequisito(
                separacion,
                "Comprobante de separación",
                "Recibo o boleta que acredita que el cliente pagó el monto de separación de la unidad.",
                "receipt_long",
                "No note"
                ));

        separacion.getRequisitos().add(construirRequisito(
                separacion,
                "Ficha del cliente",
                "Registro con los datos personales del comprador: nombre completo, DNI, teléfono, correo y datos del bien adquirido.",
                "person_check",
                "Información verificada por el área legal. Estos datos se utilizarán para la redacción final del contrato de compra-venta."
                ));

        etapas.add(separacion);

        // 2. ETAPA: CONTRATO
        EtapaExpediente contratoEtapa = construirEtapa(contrato, EtapaProceso.CONTRATO);

        if (tipoFinanciamiento.contains("HIPOT")) {
            contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Revisión del contrato", 1,
                    "El cliente recibe y revisa el borrador del contrato"));
            contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Carta de aprobación del banco", 2,
                    "Documento emitido por la entidad bancaria que aprueba el crédito hipotecario del cliente."));
            contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Aprobación del contrato", 3,
                    "El cliente confirma que está conforme con las condiciones"));
            contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Pago de la cuota inicial", 4,
                    "Pago de la cuota inicial pactada al firmar el contrato"));
            contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Firma del contrato", 5,
                    "Firma del contrato de compraventa"));
        } else {
            contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Revisión del contrato", 1,
                    "El cliente recibe y revisa el borrador del contrato"));
            contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Aprobación del contrato", 2,
                    "El cliente confirma que está conforme con las condiciones"));
            contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Pago de la cuota inicial", 3,
                    "Pago de la cuota inicial pactada al firmar el contrato"));
            contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Firma del contrato", 4,
                    "Firma del contrato de compraventa"));
        }

        contratoEtapa.getRequisitos().add(construirRequisito(
                contratoEtapa,
                "Contrato de compraventa (CV)",
                "Documento legal que formaliza la compra de la unidad inmobiliaria entre Llosa Edificaciones y el cliente.",
                "gavel",
                "Requiere la firma legal legalizada de ambas partes para iniciar la elevación a registros públicos."
        ));

        contratoEtapa.getRequisitos().add(construirRequisito(
                contratoEtapa,
                "Adenda (Opcional)",
                "Documento que modifica o amplía el contrato original ya firmado. Puede cambiar montos, fechas u otras condiciones pactadas.",
                "note_add",
                "Solo se genera en caso existan acuerdos posteriores modificatorios al contrato original base."
        ));

        contratoEtapa.getRequisitos().add(construirRequisito(
                contratoEtapa,
                "Cronograma de pagos",
                "Documento que establece el plan de pagos detallado, incluyendo fechas de vencimiento, montos y conceptos de cada cuota asociada al contrato.",
                "payments",
                "Se actualiza automáticamente según el avance de la construcción y la modalidad de crédito seleccionada."
        ));

        contratoEtapa.getRequisitos().add(construirRequisito(
                contratoEtapa,
                PAGO_INCIAL,
                "Comprobante del pago de la cuota inicial para formalizar la compra.",
                "payments",
                "El comprobante se sube desde la sección de pagos y se refleja automáticamente aquí."
        ));

        if (tipoFinanciamiento.contains("HIPOT")) {
            contratoEtapa.getRequisitos().add(construirRequisito(
                    contratoEtapa,
                    "Carta de aprobación del banco",
                    "Documento emitido por la entidad bancaria que aprueba el crédito hipotecario del cliente.",
                    "verified_user",
                    "Solo aplica a crédito hipotecario. Debe ser entregada antes de la firma del contrato."
            ));
        }
        etapas.add(contratoEtapa);

        // 3. ETAPA: PAGO
        EtapaExpediente pago = construirEtapa(contrato, EtapaProceso.PAGO);

        if (tipoFinanciamiento.contains("HIPOT")) {

            pago.getHitosComerciales().add(construirHito(pago, "Pago de Separación", 1,
                    "Pago de reserva de la unidad inmobiliaria."));
            pago.getHitosComerciales().add(construirHito(pago, PAGO_INCIAL, 2, "Abono inicial requerido para iniciar el proceso de compra."));
            pago.getHitosComerciales().add(construirHito(pago, "Inicio de Desembolso", 3, "El banco inicia el proceso de desembolso del crédito hipotecario."));
            pago.getHitosComerciales().add(construirHito(pago, "Minuta en Notaría", 4, "La minuta es revisada y procesada por la notaría."));
            pago.getHitosComerciales().add(construirHito(pago, "Firma de Escritura Pública", 5, "Las partes firman la escritura pública ante notario."));
            pago.getHitosComerciales().add(construirHito(pago, "Desembolso Completado", 6, "El banco realiza el desembolso final a la inmobiliaria."));

        } else {

            pago.getHitosComerciales().add(construirHito(pago, "Pago de Separación", 1,
                    "Pago de reserva de la unidad inmobiliaria."));
            pago.getHitosComerciales().add(construirHito(pago, PAGO_INCIAL, 2, "Cuota inicial requerida para formalizar la compra."));
            pago.getHitosComerciales().add(construirHito(pago, "Pago en Proceso", 3, "El cliente continúa realizando los pagos acordados."));
            pago.getHitosComerciales().add(construirHito(pago, "Pago Completado", 4, "La totalidad del importe acordado ha sido cancelada."));
        }

        etapas.add(pago);



        // 4. ETAPA: ENTREGA
        EtapaExpediente entrega = construirEtapa(contrato, EtapaProceso.ENTREGA);
        entrega.getHitosComerciales().add(construirHito(entrega, "Inmueble terminado", 1,
                "La obra de la unidad está concluida"));
        entrega.getHitosComerciales().add(construirHito(entrega, "Inmueble cancelado", 2,
                "Todos los pagos fueron completados o el desembolso fue confirmado"));
        entrega.getHitosComerciales().add(construirHito(entrega, "Comunicación de departamento terminado", 3,
                "Llosa notifica al cliente que su unidad está lista"));
        entrega.getHitosComerciales().add(construirHito(entrega, "Confirmación de fecha de entrega", 4,
                "Se coordina con el cliente la fecha y hora de entrega"));
        entrega.getHitosComerciales().add(construirHito(entrega, "Entrega del inmueble", 5,
                "El cliente recibe las llaves y firma el acta de entrega"));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Planos As Built",
                "Planos finales del departamento tal como quedó construido, con arquitectura, estructuras, sanitarias, eléctricas, mecánicas y de gas.",
                "architecture",
                NO_NOTE_CORPORATIVA
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Acta de entrega",
                "Documento firmado por el cliente y Llosa que certifica la entrega de la unidad en la fecha pactada y en condiciones acordadas.",
                "done_all",
                "Se formaliza con la firma del acta y la entrega de llaves. Revisa las observaciones antes de firmar."
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Manual del propietario",
                "Guía completa sobre el funcionamiento, mantenimiento y uso correcto de la unidad y sus instalaciones.",
                "book",
                NO_NOTE_CORPORATIVA
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Manual de convivencia",
                "Reglamento interno del edificio con normas de uso de áreas comunes, horarios, restricciones y obligaciones de los residentes.",
                "groups",
                NO_NOTE_CORPORATIVA
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Manual de Calidad Cloud",
                "Guía de uso de la plataforma de gestión postventa de Llosa para reportar observaciones sobre la unidad.",
                "cloud",
                NO_NOTE_CORPORATIVA
        ));


        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Manual de saneamiento",
                "Guía que explica el proceso de independización y registro de la propiedad.",
                "cleaning_services",
                "Documento informativo sobre el avance del proceso de saneamiento registral."
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Cartas de garantía",
                "Documentos emitidos por Llosa o proveedores que garantizan materiales y equipos instalados en la unidad.",
                "verified_user",
                "Incluye periodos de garantía para acabados, equipos y sistemas instalados."
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Fichas técnicas",
                "Especificaciones técnicas detalladas de materiales, equipos y sistemas instalados en la unidad.",
                "description",
                NO_NOTE_CORPORATIVA
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Lista de proveedores",
                "Directorio de proveedores de los principales materiales y equipos del departamento.",
                "list",
                NO_NOTE_CORPORATIVA
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Cuponera",
                "Documento con cupones o beneficios para servicios o compras relacionados con la implementación del departamento.",
                "receipt",
                NO_NOTE_CORPORATIVA
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Checklist de implementación",
                "Lista de artículos recomendados para implementar en el departamento antes de la entrega final.",
                "checklist",
                NO_NOTE_CORPORATIVA
        ));
        etapas.add(entrega);

        // 5. ETAPA: SANEAMIENTO
        EtapaExpediente saneamiento = construirEtapa(contrato, EtapaProceso.SANEAMIENTO);
        saneamiento.getHitosComerciales().add(construirHito(saneamiento, "Conformidad de obra", 1,
                "Resolución municipal que certifica que la obra fue ejecutada conforme"));
        saneamiento.getHitosComerciales().add(construirHito(saneamiento, "Declaratoria de fábrica", 2,
                "Documento legal que inscribe la edificación construida"));
        saneamiento.getHitosComerciales().add(construirHito(saneamiento, "Reglamento interno", 3,
                "Documento que define áreas comunes, privadas y reglas de convivencia"));
        saneamiento.getHitosComerciales().add(construirHito(saneamiento, "Independización del inmueble en municipalidad", 4,
                "Trámite de independización a nivel municipal"));
        saneamiento.getHitosComerciales().add(construirHito(saneamiento, "Transferencia del inmueble a nivel municipal", 5,
                "Formalización del paso municipal del proceso"));
        saneamiento.getHitosComerciales().add(construirHito(saneamiento, "Independización del inmueble ante Registros Públicos (SUNARP)", 6,
                "Trámite de independización ante SUNARP"));
        saneamiento.getHitosComerciales().add(construirHito(saneamiento, "Transferencia del inmueble a nivel registral", 7,
                "Inscripción final del inmueble a nombre del cliente"));

        saneamiento.getRequisitos().add(construirRequisito(
                saneamiento,
                "Conformidad de obra",
                "Resolución municipal que certifica que la construcción del edificio fue realizada conforme a los planos y licencias aprobadas.",
                "verified",
                "Emitido por la municipalidad. Este documento es el primer hito en el proceso de saneamiento."
        ));

        saneamiento.getRequisitos().add(construirRequisito(
                saneamiento,
                "Declaratoria de fábrica",
                "Documento legal que inscribe en SUNARP la edificación construida sobre el terreno, con sus características técnicas.",
                "gavel",
                "Inscrita en los registros públicos. Contiene información detallada de la estructura del edificio."
        ));

        saneamiento.getRequisitos().add(construirRequisito(
                saneamiento,
                "Reglamento interno",
                "Documento que establece la división de áreas comunes y privadas del edificio, y las normas de convivencia entre propietarios.",
                "description",
                "No tiene nota corporativa"
        ));

        saneamiento.getRequisitos().add(construirRequisito(
                saneamiento,
                "Partida registral del inmueble independizado",
                "Documento oficial emitido por SUNARP que acredita que la unidad está inscrita como propiedad independiente a nombre del cliente.",
                "fingerprint",
                "Este documento formaliza tu propiedad de manera definitiva en SUNARP."
        ));
        etapas.add(saneamiento);
        return etapas;
    }

// =========================================================================
// MÉTODOS AUXILIARES
// =========================================================================

    private EtapaExpediente construirEtapa(UsuarioActivo contrato, EtapaProceso proceso) {
        return EtapaExpediente.builder()
                .usuarioActivo(contrato)
                .etapaProceso(proceso)
                .estado(EstadoEtapaExpediente.PENDIENTE)
                .hitosComerciales(new ArrayList<>())
                .requisitos(new ArrayList<>())
                .build();
    }

    private HitoProcesoCompra construirHito(EtapaExpediente etapa, String nombre, int orden, String descripcion) {
        return HitoProcesoCompra.builder()
                .etapaExpediente(etapa)
                .nombreHito(nombre)
                .descripcion(descripcion)
                .orden(orden)
                .estado(EstadoHitoComercial.PENDIENTE)
                .build();
    }
    private RequisitoDocumental construirRequisito(EtapaExpediente etapa, String titulo, String descripcion, String icono, String notaCorp) {
        return RequisitoDocumental.builder()
                .etapaExpediente(etapa)
                .titulo(titulo)
                .descripcion(descripcion)
                .icono(icono)
                .notaCorporativa(notaCorp)
                .estado(EtapaRequisitoDocumental.PENDIENTE)
                .build();
    }
}
