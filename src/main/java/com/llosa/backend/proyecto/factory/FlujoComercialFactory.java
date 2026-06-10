package com.llosa.backend.proyecto.factory;

import com.llosa.backend.comercial.entity.EtapaExpediente;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
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
    public List<EtapaExpediente> generarEtapasPorDefecto(UsuarioActivo contrato) {

        List<EtapaExpediente> etapas = new ArrayList<>();

        String tipoFinanciamiento = contrato.getTipoFinanciamiento() == null
                ? ""
                : contrato.getTipoFinanciamiento().toUpperCase(Locale.ROOT);

        // 1. ETAPA: SEPARACIÓN
        EtapaExpediente separacion = construirEtapa(contrato, EtapaProceso.SEPARACION, "EN_CURSO");
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
        EtapaExpediente contratoEtapa = construirEtapa(contrato, EtapaProceso.CONTRATO, "PENDIENTE");
        contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Revisión del contrato", 1,
                "El cliente recibe y revisa el borrador del contrato"));
        contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Carta de aprobación del banco", 2,
                "Solo aplica a crédito hipotecario"));
        contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Aprobación del contrato", 3,
                "El cliente confirma que está conforme con las condiciones"));
        contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Pago de la cuota inicial", 4,
                "Pago de la cuota inicial pactada al firmar el contrato"));
        contratoEtapa.getHitosComerciales().add(construirHito(contratoEtapa, "Firma del contrato", 5,
                "Firma del contrato de compraventa"));


        contratoEtapa.getRequisitos().add(construirRequisito(
                contratoEtapa,
                "Contrato de compraventa (CV)",
                "Documento legal que formaliza la compra de la unidad inmobiliaria entre el cliente y Llosa Edificaciones.",
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
        etapas.add(contratoEtapa);

        // 3. ETAPA: PAGO
        EtapaExpediente pago = construirEtapa(contrato, EtapaProceso.PAGO, "PENDIENTE");
        pago.getHitosComerciales().add(construirHito(pago, "Pago de la cuota inicial", 1,
                "Paso común de inicio del cronograma de pagos"));

        if (tipoFinanciamiento.contains("HIPOT")) {
            pago.getHitosComerciales().add(construirHito(pago, "Inicio de desembolso", 2,
                    "El proceso bancario hacia Llosa ha comenzado"));
            pago.getHitosComerciales().add(construirHito(pago, "Minuta revisión en notaría", 3,
                    "La minuta fue enviada a notaría para revisión y elevación a escritura pública"));
            pago.getHitosComerciales().add(construirHito(pago, "Firma de escritura pública", 4,
                    "El cliente y Llosa firman ante notario"));
            pago.getHitosComerciales().add(construirHito(pago, "Desembolso completado", 5,
                    "El banco transfirió el monto aprobado a Llosa"));
            pago.getHitosComerciales().add(construirHito(pago, "Inmueble cancelado", 6,
                    "La operación financiera quedó cerrada"));
        } else {
            pago.getHitosComerciales().add(construirHito(pago, "Pago de la cuota 1", 2,
                    "Crédito directo"));
            pago.getHitosComerciales().add(construirHito(pago, "Pago de la cuota 2", 3,
                    "Crédito directo"));
            pago.getHitosComerciales().add(construirHito(pago, "Pago de la cuota 3", 4,
                    "Crédito directo"));
            pago.getHitosComerciales().add(construirHito(pago, "Pago de la cuota 4", 5,
                    "Crédito directo"));
            pago.getHitosComerciales().add(construirHito(pago, "Inmueble cancelado", 6,
                    "La unidad quedó totalmente cancelada"));
        }
        etapas.add(pago);



        // 4. ETAPA: ENTREGA
        EtapaExpediente entrega = construirEtapa(contrato, EtapaProceso.ENTREGA, "PENDIENTE");
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
                "No hay nota corporativa"
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
                "No hay nota corporativa"
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Manual de convivencia",
                "Reglamento interno del edificio con normas de uso de áreas comunes, horarios, restricciones y obligaciones de los residentes.",
                "groups",
                "No hay nota corporativa"
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Manual de Calidad Cloud",
                "Guía de uso de la plataforma de gestión postventa de Llosa para reportar observaciones sobre la unidad.",
                "cloud",
                "No hay nota corporativa"
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
                "No hay nota corporativa"
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Lista de proveedores",
                "Directorio de proveedores de los principales materiales y equipos del departamento.",
                "list",
                "No hay nota corporativa"
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Cuponera",
                "Documento con cupones o beneficios para servicios o compras relacionados con la implementación del departamento.",
                "receipt",
                "No hay nota corporativa"
        ));

        entrega.getRequisitos().add(construirRequisito(
                entrega,
                "Checklist de implementación",
                "Lista de artículos recomendados para implementar en el departamento antes de la entrega final.",
                "checklist",
                "No hay nota corporativa"
        ));
        etapas.add(entrega);

        // 5. ETAPA: SANEAMIENTO
        EtapaExpediente saneamiento = construirEtapa(contrato, EtapaProceso.SANEAMIENTO, "PENDIENTE");
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

    private EtapaExpediente construirEtapa(UsuarioActivo contrato, EtapaProceso proceso, String estado) {
        return EtapaExpediente.builder()
                .usuarioActivo(contrato)
                .etapaProceso(proceso)
                .estado(estado)
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
                .estado(EtapaRequisitoDocumental.PENDIENTE) // Todo documento nace pendiente por subir
                .build();
    }
}
