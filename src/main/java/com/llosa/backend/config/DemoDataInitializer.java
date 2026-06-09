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
        if (usuarioRepository.findByEmail("demo.asesor@llosa.com").isPresent()) {
            log.info("Datos demo ya existen, omitiendo inicialización.");
            return;
        }

        log.info("=== Inicializando datos demo ===");

        var rolAsesor = rolRepository.findByNombre("ASESOR").orElseThrow();
        var rolLegal = rolRepository.findByNombre("LEGAL").orElseThrow();
        var rolesTecnico = rolRepository.findByNombre("TECNICO").orElseThrow();
        var rolPostventa = rolRepository.findByNombre("POSTVENTA").orElseThrow();
        var rolCliente = rolRepository.findByNombre("CLIENTE").orElseThrow();

        var asesor = crearUsuarioFirebase("demo.asesor@llosa.com", "Demo123!", "ASESOR", "EMPLEADO", "Carlos", "Asesor", "DNI", "12345678", "999111000", rolAsesor);
        var legal = crearUsuarioFirebase("demo.legal@llosa.com", "Demo123!", "LEGAL", "EMPLEADO", "Rosa", "García", "DNI", "87654321", "999222111", rolLegal);
        var tecnico = crearUsuarioFirebase("demo.tecnico@llosa.com", "Demo123!", "TECNICO", "EMPLEADO", "Miguel", "Torres", "DNI", "45678912", "999333222", rolesTecnico);
        var postventa = crearUsuarioFirebase("demo.postventa@llosa.com", "Demo123!", "POSTVENTA", "EMPLEADO", "Ana", "Rivas", "DNI", "78912345", "999444333", rolPostventa);
        var cliente1 = crearUsuarioFirebase("demo.cliente1@llosa.com", "Demo123!", "CLIENTE", "CLIENTE", "María", "López", "DNI", "11122334", "999555444", rolCliente);
        var cliente2 = crearUsuarioFirebase("demo.cliente2@llosa.com", "Demo123!", "CLIENTE", "CLIENTE", "Pedro", "Sánchez", "CE", "P1234567", "999666555", rolCliente);
        var cliente3 = crearUsuarioFirebase("demo.cliente3@llosa.com", "Demo123!", "CLIENTE", "CLIENTE", "Lucía", "Mendoza", "DNI", "22334455", "999777666", rolCliente);

        if (asesor == null || legal == null || tecnico == null || postventa == null
                || cliente1 == null || cliente2 == null || cliente3 == null) {
            log.warn("No se pudieron crear todos los usuarios demo, abortando.");
            return;
        }

        var proyectoLO = crearProyectoLosOlivos();
        var proyectoSI = crearProyectoSanIsidro();
        var proyectoLM = crearProyectoLaMolina();
        proyectoRepository.save(proyectoLO);
        proyectoRepository.save(proyectoSI);
        proyectoRepository.save(proyectoLM);

        crearReportes(proyectoLO);
        crearReportes(proyectoSI);
        crearReportes(proyectoLM);

        var depto301 = proyectoLO.getTorres().getFirst().getPisos().get(2).getActivos().getFirst();
        var depto601 = proyectoLO.getTorres().get(1).getPisos().get(1).getActivos().getFirst();
        var depto201 = proyectoSI.getTorres().getFirst().getPisos().get(1).getActivos().getFirst();
        var depto701 = proyectoLM.getTorres().getFirst().getPisos().get(2).getActivos().getFirst();
        var depto401 = proyectoLO.getTorres().getFirst().getPisos().get(3).getActivos().getFirst();

        var ua1 = asignarActivoACliente(cliente1, depto301, null, "Crédito Hipotecario", "Separación", "Minuta Pendiente");
        var ua2 = asignarActivoACliente(cliente2, depto601, null, "Crédito Hipotecario", "Contrato", "Contrato Firmado");
        var ua3 = asignarActivoACliente(cliente3, depto201, null, "Crédito Directo", "Pagos", "Escritura en Trámite");

        var cocheraParaCliente1 = proyectoLO.getTorres().getFirst().getPisos().getFirst().getActivos().getFirst();
        cocheraParaCliente1.setEstadoComercial(EstadoComercialActivo.VENDIDO);
        var ua4 = asignarActivoACliente(cliente3, depto401, null, "Crédito Hipotecario", "Separación", "Minuta Pendiente");

        var cocheraParaCliente3 = proyectoLO.getTorres().getFirst().getPisos().getFirst().getActivos().get(1);
        cocheraParaCliente3.setEstadoComercial(EstadoComercialActivo.VENDIDO);
        var ua5 = asignarActivoACliente(List.of(cliente1, cliente2), depto701, null, "Crédito Hipotecario", "Entregado", "Partida Registral SUNARP");

        crearHitosProcesoCompra(ua1, List.of(
                hitoCompra(ua1, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 6, 1, 10, 0)),
                hitoCompra(ua1, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 6, 5, 15, 30)),
                hitoCompra(ua1, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 7, 1, 11, 0)),
                hitoCompra(ua1, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.EN_PROGRESO, null),
                hitoCompra(ua1, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua1, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua1, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua1, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua1, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.PENDIENTE, null)
        ));

        crearHitosProcesoCompra(ua2, List.of(
                hitoCompra(ua2, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 5, 10, 9, 0)),
                hitoCompra(ua2, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 5, 12, 14, 0)),
                hitoCompra(ua2, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 6, 1, 10, 0)),
                hitoCompra(ua2, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 6, 15, 16, 0)),
                hitoCompra(ua2, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 7, 1, 11, 0)),
                hitoCompra(ua2, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.EN_PROGRESO, null),
                hitoCompra(ua2, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua2, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua2, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.PENDIENTE, null)
        ));

        crearHitosProcesoCompra(ua3, List.of(
                hitoCompra(ua3, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 3, 1, 10, 0)),
                hitoCompra(ua3, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 3, 5, 15, 0)),
                hitoCompra(ua3, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 4, 1, 11, 0)),
                hitoCompra(ua3, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 4, 15, 16, 0)),
                hitoCompra(ua3, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 5, 1, 9, 0)),
                hitoCompra(ua3, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 6, 1, 14, 0)),
                hitoCompra(ua3, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 6, 15, 10, 0)),
                hitoCompra(ua3, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 6, 30, 12, 0)),
                hitoCompra(ua3, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.EN_PROGRESO, null)
        ));

        crearHitosProcesoCompra(ua4, List.of(
                hitoCompra(ua4, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 8, 1, 10, 0)),
                hitoCompra(ua4, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua4, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua4, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua4, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua4, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua4, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua4, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.PENDIENTE, null),
                hitoCompra(ua4, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.PENDIENTE, null)
        ));

        crearHitosProcesoCompra(ua5, List.of(
                hitoCompra(ua5, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 1, 15, 10, 0)),
                hitoCompra(ua5, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 1, 20, 14, 0)),
                hitoCompra(ua5, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 2, 1, 11, 0)),
                hitoCompra(ua5, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 2, 15, 16, 0)),
                hitoCompra(ua5, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 3, 1, 9, 0)),
                hitoCompra(ua5, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 4, 1, 14, 0)),
                hitoCompra(ua5, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 4, 15, 10, 0)),
                hitoCompra(ua5, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 5, 1, 12, 0)),
                hitoCompra(ua5, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.COMPLETADO, LocalDateTime.of(2025, 6, 1, 11, 0)),
                hitoCompra(ua5, EtapaProceso.SANEAMIENTO, 10, "Inscripción SUNARP", EstadoHitoComercial.EN_PROGRESO, null)
        ));

        crearRequisitosDocumentales(ua1);
        crearDocumentosParaUsuarioActivo(ua1, asesor);
        crearDocumentosParaUsuarioActivo(ua3, asesor);
        crearDocumentosParaProyecto(proyectoLO, asesor);
        crearDocumentosParaProyecto(proyectoSI, tecnico);

        crearHitosPisoDemo(proyectoLO);

        log.info("=== Datos demo inicializados correctamente ===");
    }

    private Usuario crearUsuarioFirebase(String email, String password, String rolNombre,
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

    private Proyecto crearProyectoLosOlivos() {
        var proyecto = Proyecto.builder()
                .nombre("Residencial Los Olivos")
                .descripcion("Proyecto de vivienda multifamiliar con 2 torres y 21 unidades")
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

    private Proyecto crearProyectoSanIsidro() {
        var proyecto = Proyecto.builder()
                .nombre("Edificio San Isidro")
                .descripcion("Torre corporativa con 12 pisos y 48 oficinas")
                .precertificacionEdgeLeed(true)
                .departamento("Lima")
                .distrito("San Isidro")
                .direccion("Av. Conquistadores 789")
                .fechaInicio(LocalDate.of(2025, 6, 1))
                .fechaFin(LocalDate.of(2027, 6, 30))
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
                hito(1, "Excavación", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2025, 8, 1), proyecto),
                hito(2, "Cimentación", TipoHito.OBRA, EstadoHito.COMPLETADO, LocalDate.of(2025, 10, 15), proyecto),
                hito(3, "Estructura Metálica", TipoHito.OBRA, EstadoHito.EN_PROGRESO, null, proyecto),
                hito(4, "Fachada", TipoHito.OBRA, EstadoHito.PENDIENTE, null, proyecto),
                hito(5, "Instalaciones", TipoHito.OBRA, EstadoHito.PENDIENTE, null, proyecto)
        ));

        return proyecto;
    }

    private Proyecto crearProyectoLaMolina() {
        var proyecto = Proyecto.builder()
                .nombre("Condominio La Molina")
                .descripcion("Condominio residencial con áreas verdes y 15 casas")
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

    private Activo activo(String nro, TipoActivo tipo, String areaM2, String precio, Piso piso, String linkRecorridoVirtual) {
        return Activo.builder()
                .nro(nro)
                .tipo(tipo)
                .areaM2(new BigDecimal(areaM2))
                .precio(new BigDecimal(precio))
                .estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .descripcion(nro + " - " + areaM2 + " m²")
                .linkRecorridoVirtual(linkRecorridoVirtual != null ? linkRecorridoVirtual : "")
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

    private UsuarioActivo asignarActivoACliente(Usuario cliente, Activo activo, Activo cochera,
                                                 String financiamiento, String fase, String tramiteLegal) {
        return asignarActivoACliente(List.of(cliente), activo, cochera, financiamiento, fase, tramiteLegal);
    }

    private UsuarioActivo asignarActivoACliente(List<Usuario> clientes, Activo activo, Activo cochera,
                                                 String financiamiento, String fase, String tramiteLegal) {
        activo.setEstadoComercial(
                "VENDIDO".equals(fase) || "Entregado".equals(fase) ? EstadoComercialActivo.VENDIDO :
                "Pagos".equals(fase) || "Contrato".equals(fase) ? EstadoComercialActivo.SEPARADO :
                EstadoComercialActivo.SEPARADO
        );

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
        String emails = clientes.stream().map(Usuario::getEmail).reduce((a, b) -> a + ", " + b).orElse("");
        log.info("Activo {} asignado a {} (fase={})", activo.getNro(), emails, fase);
        return ua;
    }

    private void crearHitosProcesoCompra(UsuarioActivo ua, List<HitoProcesoCompra> hitos) {
        hitoProcesoCompraRepository.saveAll(hitos);
        log.info("{} hitos de proceso de compra creados para activo {}", hitos.size(), ua.getActivo().getNro());
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

    private void crearRequisitosDocumentales(UsuarioActivo ua) {
        var hitos = hitoProcesoCompraRepository.findByUsuarioActivo_UuidUsuarioActivoOrderByOrdenAsc(
                ua.getUuidUsuarioActivo());
        for (var hito : hitos) {
            switch (hito.getEtapaProceso()) {
                case SEPARACION -> {
                    guardarRequisito(hito, "DNI del Cliente", "Copia del DNI vigente de todos los copropietarios", null, "COMPLETADO", "fas fa-id-card");
                    guardarRequisito(hito, "Voucher de Pago", "Voucher o constancia de pago de la separación", null, "COMPLETADO", "fas fa-receipt");
                }
                case CONTRATO -> {
                    guardarRequisito(hito, "Minuta de Compraventa", "Documento de minuta firmado por ambas partes", null, "EN_PROGRESO", "fas fa-file-contract");
                    guardarRequisito(hito, "Declaración Jurada de Bienes", "DDJJ de bienes y rentas del cliente", null, "PENDIENTE", "fas fa-file-signature");
                }
                case PAGO -> {
                    guardarRequisito(hito, "Cronograma de Pagos", "Cronograma detallado de cuotas", null, "PENDIENTE", "fas fa-calendar-alt");
                    guardarRequisito(hito, "Recibo de Ingresos", "Últimos 3 recibos de ingresos del cliente", null, "PENDIENTE", "fas fa-file-invoice");
                }
                case ENTREGA -> {
                    guardarRequisito(hito, "Acta de Entrega", "Documento de conformidad de entrega del inmueble", null, "PENDIENTE", "fas fa-clipboard-check");
                    guardarRequisito(hito, "Certificado de Parámetros", "Certificado de parámetros urbanísticos", null, "PENDIENTE", "fas fa-certificate");
                }
                case SANEAMIENTO -> {
                    guardarRequisito(hito, "Escritura Pública", "Escritura pública elevada a registros públicos", null, "PENDIENTE", "fas fa-file-alt");
                    guardarRequisito(hito, "Partida Registral", "Partida registral actualizada de SUNARP", null, "PENDIENTE", "fas fa-landmark");
                }
            }
        }
    }

    private void guardarRequisito(HitoProcesoCompra hito, String titulo, String descripcion,
                                   String notaCorporativa, String estado, String icono) {
        var req = RequisitoDocumental.builder()
                .hitoComercial(hito)
                .titulo(titulo)
                .descripcion(descripcion)
                .notaCorporativa(notaCorporativa)
                .estado(estado)
                .fechaEmision(LocalDate.now())
                .icono(icono)
                .build();
        requisitoDocumentalRepository.save(req);
    }

    private void crearDocumentosParaUsuarioActivo(UsuarioActivo ua, Usuario subidoPor) {
        var expedienteId = ua.getUuidUsuarioActivo().toString();

        var doc1 = Documento.builder()
                .rutaGcs("expedientes/" + expedienteId + "/contrato-compraventa.pdf")
                .nombreOriginal("Contrato de Compraventa.pdf")
                .idReferencia(expedienteId)
                .entidadReferencia("USUARIO_ACTIVO")
                .tipoDocumento(TipoDocumento.PDF_LEGAL)
                .tipoMime("application/pdf")
                .accesoRestringido(true)
                .subidoPor(subidoPor.getId())
                .build();
        documentoRepository.save(doc1);

        var doc2 = Documento.builder()
                .rutaGcs("expedientes/" + expedienteId + "/recibo-separacion.pdf")
                .nombreOriginal("Recibo de Separación.pdf")
                .idReferencia(expedienteId)
                .entidadReferencia("USUARIO_ACTIVO")
                .tipoDocumento(TipoDocumento.COMPROBANTE)
                .tipoMime("application/pdf")
                .accesoRestringido(true)
                .subidoPor(subidoPor.getId())
                .build();
        documentoRepository.save(doc2);

        log.info("Documentos creados para expediente {}", expedienteId);
    }

    private void crearDocumentosParaProyecto(Proyecto proyecto, Usuario subidoPor) {
        var proyectoId = proyecto.getId().toString();

        var doc = Documento.builder()
                .rutaGcs("proyectos/" + proyectoId + "/fotos/fachada-01.jpg")
                .nombreOriginal("fachada-principal.jpg")
                .idReferencia(proyectoId)
                .entidadReferencia("PROYECTO")
                .tipoDocumento(TipoDocumento.FOTO_OBRA)
                .tipoMime("image/jpeg")
                .accesoRestringido(false)
                .subidoPor(subidoPor.getId())
                .proyecto(proyecto)
                .build();
        documentoRepository.save(doc);

        var doc2 = Documento.builder()
                .rutaGcs("proyectos/" + proyectoId + "/videos/timelapse-01.mp4")
                .nombreOriginal("timelapse-obra.mp4")
                .idReferencia(proyectoId)
                .entidadReferencia("PROYECTO")
                .tipoDocumento(TipoDocumento.VIDEO_OBRA)
                .tipoMime("video/mp4")
                .accesoRestringido(false)
                .subidoPor(subidoPor.getId())
                .proyecto(proyecto)
                .build();
        documentoRepository.save(doc2);

        log.info("Documentos de proyecto creados para {}", proyecto.getNombre());
    }

    private void crearReportes(Proyecto proyecto) {
        var r1 = Reporte.builder()
                .proyecto(proyecto)
                .tituloPeriodo("Enero 2026")
                .porcentajeAvance(BigDecimal.valueOf(35.50))
                .descripcion("Avance general del proyecto durante el mes de enero. Se completaron las actividades de estructura en Torre A y se iniciaron los trabajos de albañilería.")
                .fecha(LocalDate.of(2026, 1, 31))
                .hitosConsolidados(List.of("Cimentación completada", "Estructura en progreso"))
                .build();
        reporteRepository.save(r1);

        var r2 = Reporte.builder()
                .proyecto(proyecto)
                .tituloPeriodo("Febrero 2026")
                .porcentajeAvance(BigDecimal.valueOf(42.00))
                .descripcion("Se avanzó con la estructura del piso 3 y se iniciaron las instalaciones eléctricas en pisos inferiores.")
                .fecha(LocalDate.of(2026, 2, 28))
                .hitosConsolidados(List.of("Estructura 60%", "Instalaciones eléctricas iniciadas"))
                .build();
        reporteRepository.save(r2);

        var r3 = Reporte.builder()
                .proyecto(proyecto)
                .tituloPeriodo("Marzo 2026")
                .porcentajeAvance(BigDecimal.valueOf(55.80))
                .descripcion("Avance significativo en acabados de Torre A. Se completaron muros y tabiques en pisos 1-3.")
                .fecha(LocalDate.of(2026, 3, 31))
                .hitosConsolidados(List.of("Muros y tabiques completados pisos 1-3", "Acabados en progreso"))
                .build();
        reporteRepository.save(r3);

        log.info("Reportes creados para {}", proyecto.getNombre());
    }

    private void crearHitosPisoDemo(Proyecto proyecto) {
        for (var torre : proyecto.getTorres()) {
            for (var piso : torre.getPisos()) {
                for (var hito : proyecto.getHitos()) {
                    if (hito.getEstado() == EstadoHito.COMPLETADO) {
                        var hp = HitoPiso.builder()
                                .piso(piso)
                                .hito(hito)
                                .estado(EstadoHito.COMPLETADO)
                                .fechaCompletado(hito.getFechaCompletado())
                                .observaciones("Completado para piso " + piso.getNroPiso())
                                .build();
                        hitoPisoRepository.save(hp);
                    } else if (hito.getEstado() == EstadoHito.EN_PROGRESO && piso.getNroPiso() <= 2) {
                        var hp = HitoPiso.builder()
                                .piso(piso)
                                .hito(hito)
                                .estado(EstadoHito.EN_PROGRESO)
                                .observaciones("En ejecución para piso " + piso.getNroPiso())
                                .build();
                        hitoPisoRepository.save(hp);
                    }
                }
            }
        }
        log.info("HitosPiso creados para {}", proyecto.getNombre());
    }
}
