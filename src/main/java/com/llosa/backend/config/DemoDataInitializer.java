package com.llosa.backend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.entity.RequisitoDocumental;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.comercial.repository.RequisitoDocumentalRepository;
import com.llosa.backend.documentos.entity.Documento;
import com.llosa.backend.documentos.enums.TipoDocumento;
import com.llosa.backend.documentos.repository.DocumentoRepository;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.HitoPiso;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Reporte;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.enums.TipoHito;
import com.llosa.backend.proyecto.repository.HitoPisoRepository;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
import com.llosa.backend.proyecto.repository.ReporteRepository;
import com.llosa.backend.proyecto.repository.UsuarioActivoRepository;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.RolRepository;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Profile("demo")
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioActivoRepository usuarioActivoRepository;
    private final HitoProcesoCompraRepository hitoProcesoCompraRepository;
    private final RequisitoDocumentalRepository requisitoDocumentalRepository;
    private final DocumentoRepository documentoRepository;
    private final ReporteRepository reporteRepository;
    private final HitoPisoRepository hitoPisoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.findByEmail("demo.admin@llosa.com").isPresent()) {
            log.info("Datos demo ya existen, omitiendo inicialización.");
            return;
        }

        log.info("=== Inicializando datos demo ===");

        var admin = crearUsuarioDemo("demo.admin@llosa.com", "Demo123!", "ADMIN", "EMPLEADO", "Admin", "Sistema");
        var asesor = crearUsuarioDemo("demo.asesor@llosa.com", "Demo123!", "ASESOR", "EMPLEADO", "Carlos", "Asesor");
        var cliente = crearUsuarioDemo("demo.cliente@llosa.com", "Demo123!", "CLIENTE", "CLIENTE", "María", "Cliente");
        
        var sofia = crearUsuarioDemo("sofia@cliente.com", "Demo123!", "CLIENTE", "CLIENTE", "Sofía", "Pérez");
        var ricardo = crearUsuarioDemo("ricardo@cliente.com", "Demo123!", "CLIENTE", "CLIENTE", "Ricardo", "Gómez");
        var carmen = crearUsuarioDemo("carmen@cliente.com", "Demo123!", "CLIENTE", "CLIENTE", "Carmen", "López");
        var tecnico = crearUsuarioDemo("tecnico@llosa.com", "Demo123!", "ASESOR", "EMPLEADO", "Juan", "Técnico");
        var rolCliente = carmen.getRol();

        if (admin == null || asesor == null || cliente == null || sofia == null || ricardo == null || carmen == null || tecnico == null) {
            log.warn("No se pudieron crear los usuarios demo, abortando.");
            return;
        }

        var proyectoLO = crearProyectoLosOlivos();
        var proyectoSI = crearProyectoSanIsidroCompletado();
        var proyectoLM = crearProyectoLaMolina();
        proyectoRepository.save(proyectoLO);
        proyectoRepository.save(proyectoSI);
        proyectoRepository.save(proyectoLM);

        crearReportes(proyectoLO);
        crearReportes(proyectoSI);
        crearReportes(proyectoLM);

        // ─── SOFÍA: Separación ─────────────────────────────────────────────
        var dep402 = proyectoLO.getTorres().getFirst().getPisos().get(3).getActivos().get(1);
        var coch3 = proyectoLO.getTorres().get(1).getPisos().getFirst().getActivos().get(0);
        var uaSofia = asignarActivo(sofia, dep402, coch3,
                "Crédito Hipotecario", "Separación", "Minuta Pendiente");
        crearHitos(uaSofia, List.of(
                hitoCompra(uaSofia, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2026, 5, 20, 10, 0)),
                hitoCompra(uaSofia, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2026, 5, 22, 15, 30)),
                hitoCompra(uaSofia, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(uaSofia, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(uaSofia, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(uaSofia, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(uaSofia, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(uaSofia, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(uaSofia, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.PENDIENTE, null)
        ));
        crearRequisitos(uaSofia);
        crearDocumentosExpediente(uaSofia, asesor, true);

        // ─── RICARDO: Estado Avanzado, 1 propiedad ─────────────────────────
        var casa101 = proyectoLM.getTorres().getFirst().getPisos().getFirst().getActivos().getFirst();
        var uaRicardo = asignarActivo(ricardo, casa101, null,
                "Crédito Hipotecario", "Entregado", "Partida Registral SUNARP");
        crearHitos(uaRicardo, List.of(
                hitoCompra(uaRicardo, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 9, 1, 10, 0)),
                hitoCompra(uaRicardo, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 9, 5, 14, 0)),
                hitoCompra(uaRicardo, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 10, 1, 11, 0)),
                hitoCompra(uaRicardo, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 10, 20, 16, 0)),
                hitoCompra(uaRicardo, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 11, 10, 9, 0)),
                hitoCompra(uaRicardo, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 12, 15, 14, 0)),
                hitoCompra(uaRicardo, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2026, 1, 10, 10, 0)),
                hitoCompra(uaRicardo, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2026, 1, 30, 12, 0)),
                hitoCompra(uaRicardo, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.EN_PROGRESO, null),
                hitoCompra(uaRicardo, EtapaProceso.SANEAMIENTO, 10, "Inscripción SUNARP", EstadoHitoComercial.PENDIENTE, null)
        ));
        crearRequisitos(uaRicardo);
        crearDocumentosExpediente(uaRicardo, asesor, false);

        // ─── CARMEN: Múltiples propiedades, 1 proyecto terminado ───────────
        var of201 = proyectoSI.getTorres().getFirst().getPisos().get(1).getActivos().getFirst();
        var uaCarmen1 = asignarActivo(carmen, of201, null,
                "Crédito Directo", "Entregado", "Partida Registral SUNARP");
        crearHitos(uaCarmen1, List.of(
                hitoCompra(uaCarmen1, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 3, 1, 10, 0)),
                hitoCompra(uaCarmen1, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 3, 5, 14, 0)),
                hitoCompra(uaCarmen1, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 4, 1, 11, 0)),
                hitoCompra(uaCarmen1, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 4, 20, 16, 0)),
                hitoCompra(uaCarmen1, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 5, 15, 9, 0)),
                hitoCompra(uaCarmen1, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 6, 1, 14, 0)),
                hitoCompra(uaCarmen1, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 6, 20, 10, 0)),
                hitoCompra(uaCarmen1, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 7, 15, 12, 0)),
                hitoCompra(uaCarmen1, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 8, 1, 11, 0)),
                hitoCompra(uaCarmen1, EtapaProceso.SANEAMIENTO, 10, "Inscripción SUNARP", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 9, 1, 11, 0))
        ));

        var dep702 = proyectoLO.getTorres().get(1).getPisos().get(2).getActivos().get(1);
        var coch5 = proyectoLO.getTorres().get(1).getPisos().getFirst().getActivos().get(2);
        var uaCarmen2 = asignarActivo(carmen, dep702, coch5,
                "Crédito Hipotecario", "Contrato", "Contrato Firmado");
        crearHitos(uaCarmen2, List.of(
                hitoCompra(uaCarmen2, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2026, 4, 1, 10, 0)),
                hitoCompra(uaCarmen2, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2026, 4, 8, 15, 0)),
                hitoCompra(uaCarmen2, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2026, 5, 1, 11, 0)),
                hitoCompra(uaCarmen2, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.EN_PROGRESO, null),
                hitoCompra(uaCarmen2, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(uaCarmen2, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(uaCarmen2, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(uaCarmen2, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(uaCarmen2, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.PENDIENTE, null)
        ));
        crearRequisitos(uaCarmen2);
        crearDocumentosExpediente(uaCarmen1, asesor, false);
        crearDocumentosExpediente(uaCarmen2, asesor, true);

        var carmenJoint = crearUsuario("demo.cliente3b@llosa.com", "Demo123!", "CLIENTE", "CLIENTE", "Carmen", "Vega de la Cruz", "DNI", "22334456", "999777667", rolCliente);
        if (carmenJoint != null) {
            var dep301 = proyectoLO.getTorres().getFirst().getPisos().get(2).getActivos().getFirst();
            var uaJoint = asignarActivoMulti(List.of(carmen, carmenJoint), dep301, null,
                    "Crédito Hipotecario", "Separación", "Minuta Pendiente");
            crearHitos(uaJoint, List.of(
                    hitoCompra(uaJoint, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2026, 6, 1, 10, 0)),
                    hitoCompra(uaJoint, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.PENDIENTE, null),
                    hitoCompra(uaJoint, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.PENDIENTE, null),
                    hitoCompra(uaJoint, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.PENDIENTE, null),
                    hitoCompra(uaJoint, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.PENDIENTE, null),
                    hitoCompra(uaJoint, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.PENDIENTE, null),
                    hitoCompra(uaJoint, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.PENDIENTE, null),
                    hitoCompra(uaJoint, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.PENDIENTE, null),
                    hitoCompra(uaJoint, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.PENDIENTE, null)
            ));
            crearRequisitos(uaJoint);
            crearDocumentosExpediente(uaJoint, asesor, true);
        }

        crearDocumentosParaProyecto(proyectoLO, tecnico);
        crearDocumentosParaProyecto(proyectoSI, tecnico);
        crearDocumentosParaProyecto(proyectoLM, tecnico);

        crearHitosPisoDemo(proyectoLO);
        crearHitosPisoDemo(proyectoSI);
        crearHitosPisoDemo(proyectoLM);

        var depto301 = proyectoLO.getTorres().getFirst().getPisos().get(2).getActivos().get(0);
        var usuarioActivo = uaRicardo;

        log.info("=== Datos demo inicializados correctamente ===");
        log.info("Usuario admin:   demo.admin@llosa.com / Demo123!  (rol=ADMIN)");
        log.info("Usuario asesor:  demo.asesor@llosa.com / Demo123!  (rol=ASESOR)");
        log.info("Usuario cliente: demo.cliente@llosa.com / Demo123! (rol=CLIENTE)");
        log.info("Activo asignado al cliente: {} (id={})", depto301.getNro(), depto301.getId());
        log.info("UUID UsuarioActivo (uuidUsuarioActivo): {}", usuarioActivo.getUuidUsuarioActivo());
    }

    private Usuario crearUsuarioDemo(String email, String password, String rolNombre,
                                     String tipoUsuario, String nombre, String apellidos) {
        var rol = rolRepository.findByNombre(rolNombre)
                .orElseGet(() -> {
                    var newRol = new Rol();
                    newRol.setNombre(rolNombre);
                    return rolRepository.save(newRol);
                });

        return crearUsuario(email, password, rolNombre, tipoUsuario, nombre, apellidos,
                "DNI", UUID.randomUUID().toString().substring(0, 8), "999888777", rol);
    }

    private Usuario crearUsuario(String email, String password, String rolNombre,
                                  String tipoUsuario, String nombre, String apellidos,
                                  String tipoDoc, String numDoc, String telefono, Rol rol) {
        String firebaseUid;
        try {
            var request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(nombre + " " + apellidos);
            var record = FirebaseAuth.getInstance().createUser(request);
            firebaseUid = record.getUid();
            log.info("Usuario Firebase creado: {}", email);
        } catch (FirebaseAuthException e) {
            if (e.getMessage() != null && e.getMessage().contains("EMAIL_EXISTS")) {
                try {
                    var existing = FirebaseAuth.getInstance().getUserByEmail(email);
                    firebaseUid = existing.getUid();
                    log.info("Usuario Firebase ya existe: {}", email);
                } catch (FirebaseAuthException ex) {
                    log.error("Error al obtener usuario Firebase existente: {}", ex.getMessage());
                    return null;
                }
            } else {
                log.warn("Error al crear usuario Firebase: {} - {}", email, e.getMessage());
                return null;
            }
        }

        var usuario = new Usuario();
        usuario.setFirebaseUuid(firebaseUid);
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setApellidos(apellidos);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setDocumentoIdentidad(tipoDoc + "-" + numDoc);
        usuario.setTelefono(telefono);
        usuarioRepository.save(usuario);
        log.info("Usuario BD creado: {} (rol={})", email, rolNombre);

        return usuario;
    }

    // ─── PROYECTOS ─────────────────────────────────────────────────────────────

    private Proyecto crearProyectoLosOlivos() {
        var proyecto = Proyecto.builder()
                .nombre("Residencial Los Olivos")
                .descripcion("Proyecto de vivienda multifamiliar con 2 torres y 21 unidades. " +
                        "Cuenta con áreas verdes, estacionamiento y seguridad 24h.")
                .precertificacionEdgeLeed(true)
                .departamento("Lima")
                .distrito("Los Olivos")
                .direccion("Av. Universitaria 1234")
                .fechaInicio(LocalDate.of(2025, 1, 15))
                .fechaFin(LocalDate.of(2026, 12, 30))
                .build();

        var torreA = Torre.builder().nombre("Torre A").proyecto(proyecto).build();
        var torreB = Torre.builder().nombre("Torre B").proyecto(proyecto).build();

        var pisoAm1 = Piso.builder().nroPiso(-1).torre(torreA).build();
        pisoAm1.setActivos(List.of(
                activo("COCHERA 1", TipoActivo.COCHERA, "12.5", "5000", pisoAm1, "https://tour-virtual.llosa.com/los-olivos/coch-1"),
                activo("COCHERA 2", TipoActivo.COCHERA, "12.5", "5000", pisoAm1, "https://tour-virtual.llosa.com/los-olivos/coch-2"),
                activo("DEPOSITO 101", TipoActivo.DEPOSITO, "8.0", "3000", pisoAm1, null)
        ));

        var pisoA1 = Piso.builder().nroPiso(1).torre(torreA).build();
        pisoA1.setActivos(List.of(
                activo("DEPARTAMENTO 201", TipoActivo.DEPARTAMENTO, "80.0", "180000", pisoA1, "https://tour-virtual.llosa.com/los-olivos/dep-201"),
                activo("DEPARTAMENTO 202", TipoActivo.DEPARTAMENTO, "85.0", "185000", pisoA1, "https://tour-virtual.llosa.com/los-olivos/dep-202")
        ));

        var pisoA2 = Piso.builder().nroPiso(2).torre(torreA).build();
        pisoA2.setActivos(List.of(
                activo("DEPARTAMENTO 301", TipoActivo.DEPARTAMENTO, "80.0", "182000", pisoA2, "https://tour-virtual.llosa.com/los-olivos/dep-301"),
                activo("DEPARTAMENTO 302", TipoActivo.DEPARTAMENTO, "85.0", "187000", pisoA2, "https://tour-virtual.llosa.com/los-olivos/dep-302")
        ));

        var pisoA3 = Piso.builder().nroPiso(3).torre(torreA).build();
        pisoA3.setActivos(List.of(
                activo("DEPARTAMENTO 401", TipoActivo.DEPARTAMENTO, "90.0", "192000", pisoA3, "https://tour-virtual.llosa.com/los-olivos/dep-401"),
                activo("DEPARTAMENTO 402", TipoActivo.DEPARTAMENTO, "95.0", "197000", pisoA3, "https://tour-virtual.llosa.com/los-olivos/dep-402")
        ));

        var pisoA4 = Piso.builder().nroPiso(4).torre(torreA).build();
        pisoA4.setActivos(List.of(
                activo("DEPARTAMENTO 501", TipoActivo.DEPARTAMENTO, "100.0", "200000", pisoA4, "https://tour-virtual.llosa.com/los-olivos/dep-501"),
                activo("DEPARTAMENTO 502", TipoActivo.DEPARTAMENTO, "105.0", "205000", pisoA4, "https://tour-virtual.llosa.com/los-olivos/dep-502")
        ));

        torreA.setPisos(List.of(pisoAm1, pisoA1, pisoA2, pisoA3, pisoA4));

        var pisoBm1 = Piso.builder().nroPiso(-1).torre(torreB).build();
        pisoBm1.setActivos(List.of(
                activo("COCHERA 3", TipoActivo.COCHERA, "12.5", "5000", pisoBm1, "https://tour-virtual.llosa.com/los-olivos/coch-3"),
                activo("COCHERA 4", TipoActivo.COCHERA, "12.5", "5000", pisoBm1, "https://tour-virtual.llosa.com/los-olivos/coch-4"),
                activo("COCHERA 5", TipoActivo.COCHERA, "12.5", "5000", pisoBm1, "https://tour-virtual.llosa.com/los-olivos/coch-5"),
                activo("DEPOSITO 102", TipoActivo.DEPOSITO, "8.0", "3000", pisoBm1, null)
        ));

        var pisoB1 = Piso.builder().nroPiso(1).torre(torreB).build();
        pisoB1.setActivos(List.of(
                activo("DEPARTAMENTO 601", TipoActivo.DEPARTAMENTO, "75.0", "175000", pisoB1, "https://tour-virtual.llosa.com/los-olivos/dep-601"),
                activo("DEPARTAMENTO 602", TipoActivo.DEPARTAMENTO, "78.0", "178000", pisoB1, "https://tour-virtual.llosa.com/los-olivos/dep-602")
        ));

        var pisoB2 = Piso.builder().nroPiso(2).torre(torreB).build();
        pisoB2.setActivos(List.of(
                activo("DEPARTAMENTO 701", TipoActivo.DEPARTAMENTO, "82.0", "182000", pisoB2, "https://tour-virtual.llosa.com/los-olivos/dep-701"),
                activo("DEPARTAMENTO 702", TipoActivo.DEPARTAMENTO, "85.0", "185000", pisoB2, "https://tour-virtual.llosa.com/los-olivos/dep-702")
        ));

        var pisoB3 = Piso.builder().nroPiso(3).torre(torreB).build();
        pisoB3.setActivos(List.of(
                activo("DEPARTAMENTO 801", TipoActivo.DEPARTAMENTO, "95.0", "195000", pisoB3, "https://tour-virtual.llosa.com/los-olivos/dep-801"),
                activo("DEPARTAMENTO 802", TipoActivo.DEPARTAMENTO, "100.0", "200000", pisoB3, "https://tour-virtual.llosa.com/los-olivos/dep-802")
        ));

        torreB.setPisos(List.of(pisoBm1, pisoB1, pisoB2, pisoB3));

        proyecto.setTorres(List.of(torreA, torreB));
        proyecto.setHitos(List.of(
                hito(1, "Cimentación", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2025, 3, 15), proyecto),
                hito(2, "Estructura", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2025, 6, 30), proyecto),
                hito(3, "Muros y Tabiques", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2025, 9, 15), proyecto),
                hito(4, "Acabados", TipoHito.OBRA, EstadoHito.EN_PROGRESO, null, proyecto),
                hito(5, "Instalaciones Eléctricas y Sanitarias", TipoHito.OBRA, EstadoHito.EN_PROGRESO, null, proyecto),
                hito(6, "Áreas Comunes", TipoHito.OBRA, EstadoHito.PENDIENTE, null, proyecto),
                hito(7, "Saneamiento Legal", TipoHito.SANEAMIENTO, EstadoHito.PENDIENTE, null, proyecto)
        ));

        return proyecto;
    }

    private Proyecto crearProyectoSanIsidroCompletado() {
        var proyecto = Proyecto.builder()
                .nombre("Edificio San Isidro")
                .descripcion("Torre corporativa moderna con 3 pisos y 7 oficinas. " +
                        "Proyecto completamente terminado y entregado.")
                .precertificacionEdgeLeed(true)
                .departamento("Lima")
                .distrito("San Isidro")
                .direccion("Av. Conquistadores 789")
                .fechaInicio(LocalDate.of(2024, 6, 1))
                .fechaFin(LocalDate.of(2025, 12, 31))
                .build();

        var torreUnica = Torre.builder().nombre("Torre Única").proyecto(proyecto).build();

        var piso1 = Piso.builder().nroPiso(1).torre(torreUnica).build();
        piso1.setActivos(List.of(
                activo("OFICINA 101", TipoActivo.DEPARTAMENTO, "120.0", "240000", piso1, "https://tour-virtual.llosa.com/san-isidro/of-101"),
                activo("OFICINA 102", TipoActivo.DEPARTAMENTO, "100.0", "200000", piso1, "https://tour-virtual.llosa.com/san-isidro/of-102"),
                activo("OFICINA 103", TipoActivo.DEPARTAMENTO, "150.0", "300000", piso1, "https://tour-virtual.llosa.com/san-isidro/of-103")
        ));

        var piso2 = Piso.builder().nroPiso(2).torre(torreUnica).build();
        piso2.setActivos(List.of(
                activo("OFICINA 201", TipoActivo.DEPARTAMENTO, "130.0", "260000", piso2, "https://tour-virtual.llosa.com/san-isidro/of-201"),
                activo("OFICINA 202", TipoActivo.DEPARTAMENTO, "110.0", "220000", piso2, "https://tour-virtual.llosa.com/san-isidro/of-202")
        ));

        var piso3 = Piso.builder().nroPiso(3).torre(torreUnica).build();
        piso3.setActivos(List.of(
                activo("OFICINA 301", TipoActivo.DEPARTAMENTO, "200.0", "400000", piso3, "https://tour-virtual.llosa.com/san-isidro/of-301"),
                activo("OFICINA 302", TipoActivo.DEPARTAMENTO, "180.0", "360000", piso3, "https://tour-virtual.llosa.com/san-isidro/of-302")
        ));

        torreUnica.setPisos(List.of(piso1, piso2, piso3));

        proyecto.setTorres(List.of(torreUnica));
        proyecto.setHitos(List.of(
                hito(1, "Excavación y Movimiento de Tierras", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2024, 8, 1), proyecto),
                hito(2, "Cimentación", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2024, 10, 15), proyecto),
                hito(3, "Estructura Metálica y Concreto", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2025, 1, 15), proyecto),
                hito(4, "Fachada y Acabados", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2025, 6, 30), proyecto),
                hito(5, "Instalaciones Eléctricas y Sanitarias", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2025, 8, 15), proyecto),
                hito(6, "Áreas Comunes y Entrega", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2025, 11, 30), proyecto),
                hito(7, "Saneamiento Legal", TipoHito.SANEAMIENTO, EstadoHito.COMPLETADO, LocalDate.of(2025, 12, 31), proyecto)
        ));

        return proyecto;
    }

    private Proyecto crearProyectoLaMolina() {
        var proyecto = Proyecto.builder()
                .nombre("Condominio La Molina")
                .descripcion("Condominio residencial con áreas verdes, 6 casas dúplex " +
                        "y piscina comunitaria. Entrega estimada 2028.")
                .precertificacionEdgeLeed(false)
                .departamento("Lima")
                .distrito("La Molina")
                .direccion("Av. La Fontana 567")
                .fechaInicio(LocalDate.of(2026, 1, 15))
                .fechaFin(LocalDate.of(2028, 3, 30))
                .build();

        var torreNorte = Torre.builder().nombre("Ala Norte").proyecto(proyecto).build();

        var piso1 = Piso.builder().nroPiso(1).torre(torreNorte).build();
        piso1.setActivos(List.of(
                activo("CASA 101", TipoActivo.DEPARTAMENTO, "150.0", "350000", piso1, "https://tour-virtual.llosa.com/la-molina/casa-101"),
                activo("CASA 102", TipoActivo.DEPARTAMENTO, "160.0", "370000", piso1, "https://tour-virtual.llosa.com/la-molina/casa-102")
        ));

        var piso2 = Piso.builder().nroPiso(2).torre(torreNorte).build();
        piso2.setActivos(List.of(
                activo("CASA 201", TipoActivo.DEPARTAMENTO, "140.0", "330000", piso2, "https://tour-virtual.llosa.com/la-molina/casa-201"),
                activo("CASA 202", TipoActivo.DEPARTAMENTO, "155.0", "355000", piso2, "https://tour-virtual.llosa.com/la-molina/casa-202")
        ));

        var piso3 = Piso.builder().nroPiso(3).torre(torreNorte).build();
        piso3.setActivos(List.of(
                activo("CASA 301", TipoActivo.DEPARTAMENTO, "180.0", "420000", piso3, "https://tour-virtual.llosa.com/la-molina/casa-301"),
                activo("CASA 302", TipoActivo.DEPARTAMENTO, "170.0", "400000", piso3, "https://tour-virtual.llosa.com/la-molina/casa-302")
        ));

        torreNorte.setPisos(List.of(piso1, piso2, piso3));

        proyecto.setTorres(List.of(torreNorte));
        proyecto.setHitos(List.of(
                hito(1, "Diseño y Permisos", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2026, 3, 1), proyecto),
                hito(2, "Movimiento de Tierras", TipoHito.OBRA, EstadoHito.EN_PROGRESO, null, proyecto),
                hito(3, "Cimentación", TipoHito.OBRA, EstadoHito.PENDIENTE, null, proyecto),
                hito(4, "Construcción de Casas", TipoHito.OBRA, EstadoHito.PENDIENTE, null, proyecto),
                hito(5, "Áreas Comunes y Cierre", TipoHito.OBRA, EstadoHito.PENDIENTE, null, proyecto)
        ));

        return proyecto;
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────────

    private Activo activo(String nro, TipoActivo tipo, String areaM2, String precio, Piso piso, String link) {
        return Activo.builder()
                .nro(nro)
                .tipo(tipo)
                .areaM2(new BigDecimal(areaM2))
                .precio(new BigDecimal(precio))
                .estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .descripcion(nro + " - " + areaM2 + " m²")
                .linkRecorridoVirtual(link != null ? link : "")
                .piso(piso)
                .build();
    }

    private Hito hito(Integer orden, String titulo, TipoHito tipo, EstadoHito estado,
                      LocalDate fechaCompletado, Proyecto proyecto) {
        return Hito.builder()
                .orden(orden)
                .titulo(titulo)
                .tipo(tipo)
                .estado(estado)
                .fechaCompletado(fechaCompletado)
                .proyecto(proyecto)
                .build();
    }

    // ─── USUARIO ACTIVO ─────────────────────────────────────────────────────────

    private UsuarioActivo asignarActivo(Usuario cliente, Activo activo, Activo cochera,
                                         String financiamiento, String fase, String tramiteLegal) {
        return asignarActivoMulti(List.of(cliente), activo, cochera, financiamiento, fase, tramiteLegal);
    }

    private UsuarioActivo asignarActivoMulti(List<Usuario> clientes, Activo activo, Activo cochera,
                                              String financiamiento, String fase, String tramiteLegal) {
        var estadoComercial = "Entregado".equals(fase)
                ? EstadoComercialActivo.VENDIDO
                : EstadoComercialActivo.SEPARADO;
        activo.setEstadoComercial(estadoComercial);

        var ua = UsuarioActivo.builder()
                .activo(activo)
                .cochera(cochera)
                .tipoFinanciamiento(financiamiento)
                .faseComercial(fase)
                .estadoTramiteLegal(tramiteLegal)
                .fechaAdquisicion(LocalDateTime.now())
                .build();
        ua.setClientes(clientes);
        usuarioActivoRepository.save(ua);

        if (cochera != null) {
            cochera.setEstadoComercial(EstadoComercialActivo.VENDIDO);
        }

        String emails = clientes.stream().map(Usuario::getEmail).reduce((a, b) -> a + ", " + b).orElse("");
        log.info("Activo {} asignado a {} (fase={})", activo.getNro(), emails, fase);
        return ua;
    }

    private void crearHitosProcesoCompra(UsuarioActivo ua) {
        var hitos = List.of(
                hitoCompra(ua, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO),
                hitoCompra(ua, EtapaProceso.CONTRATO, 2, "Revisión de Contrato", EstadoHitoComercial.EN_PROGRESO),
                hitoCompra(ua, EtapaProceso.CONTRATO, 3, "Firma de Contrato", EstadoHitoComercial.PENDIENTE),
                // Etapa PAGO se deja vacía para ser llenada por la Carta de Aprobación
                hitoCompra(ua, EtapaProceso.ENTREGA, 4, "Coordinación de Entrega", EstadoHitoComercial.PENDIENTE),
                hitoCompra(ua, EtapaProceso.ENTREGA, 5, "Acta de Entrega", EstadoHitoComercial.PENDIENTE),
                hitoCompra(ua, EtapaProceso.SANEAMIENTO, 6, "Trámite de Saneamiento", EstadoHitoComercial.PENDIENTE)
        );
        hitoProcesoCompraRepository.saveAll(hitos);
        log.info("{} hitos de compra creados para activo {}", hitos.size(), ua.getActivo().getNro());
    }

    private void crearHitos(UsuarioActivo ua, List<HitoProcesoCompra> hitos) {
        hitoProcesoCompraRepository.saveAll(hitos);
        log.info("{} hitos de compra creados para activo {}", hitos.size(), ua.getActivo().getNro());
    }

    private HitoProcesoCompra hitoCompra(UsuarioActivo ua, EtapaProceso etapa, int orden,
                                         String nombre, EstadoHitoComercial estado) {
        return hitoCompra(ua, etapa, orden, nombre, estado, null);
    }

    private HitoProcesoCompra hitoCompra(UsuarioActivo ua, EtapaProceso etapa, int orden,
                                          String nombre, EstadoHitoComercial estado,
                                          LocalDateTime fechaCompletado) {
        return HitoProcesoCompra.builder()
                .usuarioActivo(ua)
                .etapaProceso(etapa)
                .orden(orden)
                .nombreHito(nombre)
                .estado(estado)
                .fechaCompletado(fechaCompletado)
                .build();
    }

    // ─── REQUISITOS DOCUMENTALES ────────────────────────────────────────────────

    private void crearRequisitos(UsuarioActivo ua) {
        var hitos = hitoProcesoCompraRepository
                .findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(ua.getUuidUsuarioActivo());
        for (var hito : hitos) {
            switch (hito.getEtapaProceso()) {
                case SEPARACION -> {
                    req(hito, "DNI del Cliente", "Copia del DNI vigente de todos los copropietarios", null, "COMPLETADO", "fas fa-id-card");
                    req(hito, "Voucher de Pago", "Voucher o constancia de pago de la separación", null, "COMPLETADO", "fas fa-receipt");
                }
                case CONTRATO -> {
                    req(hito, "Minuta de Compraventa", "Documento de minuta firmado por ambas partes", null, "EN_PROGRESO", "fas fa-file-contract");
                    req(hito, "Declaración Jurada de Bienes", "DDJJ de bienes y rentas del cliente", null, "PENDIENTE", "fas fa-file-signature");
                }
                case PAGO -> {
                    req(hito, "Cronograma de Pagos", "Cronograma detallado de cuotas", null, "PENDIENTE", "fas fa-calendar-alt");
                    req(hito, "Recibo de Ingresos", "Últimos 3 recibos de ingresos del cliente", null, "PENDIENTE", "fas fa-file-invoice");
                    req(hito, "Evaluación Crediticia", "Reporte de central de riesgos", null, "PENDIENTE", "fas fa-chart-line");
                }
                case ENTREGA -> {
                    req(hito, "Acta de Entrega", "Documento de conformidad de entrega del inmueble", null, "PENDIENTE", "fas fa-clipboard-check");
                    req(hito, "Certificado de Parámetros", "Certificado de parámetros urbanísticos", null, "PENDIENTE", "fas fa-certificate");
                    req(hito, "Carta de Conformidad", "Carta de conformidad del cliente", null, "PENDIENTE", "fas fa-envelope");
                }
                case SANEAMIENTO -> {
                    req(hito, "Escritura Pública", "Escritura pública elevada a registros públicos", null, "PENDIENTE", "fas fa-file-alt");
                    req(hito, "Partida Registral", "Partida registral actualizada de SUNARP", null, "PENDIENTE", "fas fa-landmark");
                    req(hito, "Pago de Impuestos", "Constancia de pago de alcabala", null, "PENDIENTE", "fas fa-coins");
                }
            }
        }
    }

    private void req(HitoProcesoCompra hito, String titulo, String descripcion,
                      String notaCorporativa, String estado, String icono) {
        requisitoDocumentalRepository.save(RequisitoDocumental.builder()
                .hitoComercial(hito)
                .titulo(titulo)
                .descripcion(descripcion)
                .notaCorporativa(notaCorporativa)
                .estado(estado)
                .fechaEmision(LocalDate.now())
                .icono(icono)
                .build());
    }

    // ─── DOCUMENTOS ─────────────────────────────────────────────────────────────

    private void crearDocumentosExpediente(UsuarioActivo ua, Usuario subidoPor, boolean incluirContrato) {
        var expedienteId = ua.getUuidUsuarioActivo().toString();

        if (incluirContrato) {
            documentoRepository.save(Documento.builder()
                    .rutaGcs("expedientes/" + expedienteId + "/contrato-compraventa.pdf")
                    .nombreOriginal("Contrato de Compraventa.pdf")
                    .idReferencia(expedienteId)
                    .entidadReferencia("USUARIO_ACTIVO")
                    .tipoDocumento(TipoDocumento.PDF_LEGAL)
                    .tipoMime("application/pdf")
                    .accesoRestringido(true)
                    .subidoPor(subidoPor.getId())
                    .build());
        }

        documentoRepository.save(Documento.builder()
                .rutaGcs("expedientes/" + expedienteId + "/recibo-separacion.pdf")
                .nombreOriginal("Recibo de Separación.pdf")
                .idReferencia(expedienteId)
                .entidadReferencia("USUARIO_ACTIVO")
                .tipoDocumento(TipoDocumento.COMPROBANTE)
                .tipoMime("application/pdf")
                .accesoRestringido(true)
                .subidoPor(subidoPor.getId())
                .build());

        log.info("Documentos creados para expediente {}", expedienteId);
    }

    private void crearDocumentosParaProyecto(Proyecto proyecto, Usuario subidoPor) {
        var proyectoId = proyecto.getId().toString();

        documentoRepository.save(Documento.builder()
                .rutaGcs("proyectos/" + proyectoId + "/fotos/fachada-principal.jpg")
                .nombreOriginal("fachada-principal.jpg")
                .idReferencia(proyectoId)
                .entidadReferencia("PROYECTO")
                .tipoDocumento(TipoDocumento.FOTO_OBRA)
                .tipoMime("image/jpeg")
                .accesoRestringido(false)
                .subidoPor(subidoPor.getId())
                .proyecto(proyecto)
                .build());

        documentoRepository.save(Documento.builder()
                .rutaGcs("proyectos/" + proyectoId + "/videos/timelapse-obra.mp4")
                .nombreOriginal("timelapse-obra.mp4")
                .idReferencia(proyectoId)
                .entidadReferencia("PROYECTO")
                .tipoDocumento(TipoDocumento.VIDEO_OBRA)
                .tipoMime("video/mp4")
                .accesoRestringido(false)
                .subidoPor(subidoPor.getId())
                .proyecto(proyecto)
                .build());

        log.info("Documentos de proyecto creados para {}", proyecto.getNombre());
    }

    // ─── REPORTES ───────────────────────────────────────────────────────────────

    private void crearReportes(Proyecto proyecto) {
        var isCompletado = "COMPLETADO".equals(
                proyecto.getHitos().stream().allMatch(h -> h.getEstado() == EstadoHito.COMPLETADO)
                        ? "COMPLETADO" : "NO");

        if (proyecto.getHitos().stream().allMatch(h -> h.getEstado() == EstadoHito.COMPLETADO)) {
            reporteRepository.save(Reporte.builder()
                    .proyecto(proyecto)
                    .tituloPeriodo("Cierre de Proyecto")
                    .porcentajeAvance(BigDecimal.valueOf(100.0))
                    .descripcion("Proyecto completado al 100%. Todas las fases de construcción " +
                            "han sido finalizadas exitosamente y el saneamiento legal está al día.")
                    .fecha(LocalDate.of(2025, 12, 31))
                    .hitosConsolidados(List.of(
                            "Excavación y movimiento de tierras",
                            "Cimentación",
                            "Estructura metálica y concreto",
                            "Fachada y acabados",
                            "Instalaciones eléctricas y sanitarias",
                            "Áreas comunes y entrega",
                            "Saneamiento legal"
                    ))
                    .build());
            return;
        }

        reporteRepository.save(Reporte.builder()
                .proyecto(proyecto)
                .tituloPeriodo("Enero 2026")
                .porcentajeAvance(BigDecimal.valueOf(35.50))
                .descripcion("Avance general del proyecto durante el mes de enero. " +
                        "Se completaron las actividades de estructura y se iniciaron los trabajos de albañilería.")
                .fecha(LocalDate.of(2026, 1, 31))
                .hitosConsolidados(List.of("Cimentación completada", "Estructura en progreso"))
                .build());

        reporteRepository.save(Reporte.builder()
                .proyecto(proyecto)
                .tituloPeriodo("Febrero 2026")
                .porcentajeAvance(BigDecimal.valueOf(42.00))
                .descripcion("Avance del 42%. Se avanzó con estructura de pisos superiores " +
                        "y se iniciaron instalaciones eléctricas en pisos inferiores.")
                .fecha(LocalDate.of(2026, 2, 28))
                .hitosConsolidados(List.of("Estructura 60%", "Instalaciones eléctricas iniciadas"))
                .build());

        reporteRepository.save(Reporte.builder()
                .proyecto(proyecto)
                .tituloPeriodo("Marzo 2026")
                .porcentajeAvance(BigDecimal.valueOf(55.80))
                .descripcion("Avance significativo en acabados. " +
                        "Se completaron muros y tabiques en pisos 1-3.")
                .fecha(LocalDate.of(2026, 3, 31))
                .hitosConsolidados(List.of("Muros y tabiques completados pisos 1-3", "Acabados en progreso"))
                .build());
    }

    // ─── HITO PISO ──────────────────────────────────────────────────────────────

    private void crearHitosPisoDemo(Proyecto proyecto) {
        for (var torre : proyecto.getTorres()) {
            for (var piso : torre.getPisos()) {
                for (var hito : proyecto.getHitos()) {
                    if (hito.getEstado() == EstadoHito.COMPLETADO) {
                        hitoPisoRepository.save(HitoPiso.builder()
                                .piso(piso)
                                .hito(hito)
                                .estado(EstadoHito.COMPLETADO)
                                .fechaCompletado(hito.getFechaCompletado())
                                .observaciones("Completado para piso " + piso.getNroPiso())
                                .build());
                    } else if (hito.getEstado() == EstadoHito.EN_PROGRESO && piso.getNroPiso() <= 2) {
                        hitoPisoRepository.save(HitoPiso.builder()
                                .piso(piso)
                                .hito(hito)
                                .estado(EstadoHito.EN_PROGRESO)
                                .observaciones("En ejecución para piso " + piso.getNroPiso())
                                .build());
                    }
                }
            }
        }
        log.info("HitosPiso creados para {}", proyecto.getNombre());
    }
}
