package com.llosa.backend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.comercial.entity.HitoProcesoCompra;
import com.llosa.backend.comercial.enums.EstadoHitoComercial;
import com.llosa.backend.comercial.enums.EtapaProceso;
import com.llosa.backend.comercial.repository.HitoProcesoCompraRepository;
import com.llosa.backend.proyecto.entity.Activo;
import com.llosa.backend.proyecto.entity.Hito;
import com.llosa.backend.proyecto.entity.Piso;
import com.llosa.backend.proyecto.entity.Proyecto;
import com.llosa.backend.proyecto.entity.Torre;
import com.llosa.backend.proyecto.entity.UsuarioActivo;
import com.llosa.backend.proyecto.enums.EstadoComercialActivo;
import com.llosa.backend.proyecto.enums.EstadoHito;
import com.llosa.backend.proyecto.enums.TipoActivo;
import com.llosa.backend.proyecto.enums.TipoHito;
import com.llosa.backend.proyecto.repository.ProyectoRepository;
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

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.findByEmail("demo.asesor@llosa.com").isPresent()) {
            log.info("Datos demo ya existen, omitiendo inicialización.");
            return;
        }

        log.info("=== Inicializando datos demo ===");

        var asesor = crearUsuarioDemo("demo.asesor@llosa.com", "Demo123!", "ASESOR", "EMPLEADO", "Carlos", "Asesor");
        var cliente = crearUsuarioDemo("demo.cliente@llosa.com", "Demo123!", "CLIENTE", "CLIENTE", "María", "Cliente");
        if (asesor == null || cliente == null) {
            log.warn("No se pudieron crear los usuarios demo, abortando.");
            return;
        }

        var proyecto = crearProyectoDemo();
        proyectoRepository.save(proyecto);

        var depto301 = proyecto.getTorres().getFirst().getPisos().get(2).getActivos().getFirst();
        var usuarioActivo = asignarActivoACliente(cliente, depto301);
        crearHitosProcesoCompra(usuarioActivo);

        log.info("=== Datos demo inicializados correctamente ===");
    }

    private Usuario crearUsuarioDemo(String email, String password, String rolNombre,
                                     String tipoUsuario, String nombre, String apellidos) {
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

        var rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new RuntimeException("Rol " + rolNombre + " no encontrado"));

        var usuario = new Usuario();
        usuario.setFirebaseUuid(firebaseUid);
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setApellidos(apellidos);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setCreatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
        log.info("Usuario BD creado: {} (rol={})", email, rolNombre);

        return usuario;
    }

    private Proyecto crearProyectoDemo() {
        var proyecto = Proyecto.builder()
                .nombre("Residencial Los Olivos")
                .descripcion("Proyecto de vivienda multifamiliar con 2 torres y 21 unidades")
                .precertificacionEdgeLeed(true)
                .linkRecorridoVirtual("https://tour.virtual/llosa-olivos")
                .departamento("Lima")
                .distrito("Los Olivos")
                .direccion("Av. Universitaria 1234")
                .fechaInicio(LocalDate.of(2025, 1, 15))
                .fechaFin(LocalDate.of(2026, 12, 30))
                .build();

        var torreA = Torre.builder().nombre("Torre A").proyecto(proyecto).build();

        var pisoAm1 = Piso.builder().nroPiso(-1).torre(torreA).build();
        pisoAm1.setActivos(List.of(
                activo("COCHERA 1", TipoActivo.COCHERA, "12.5", "5000", pisoAm1),
                activo("COCHERA 2", TipoActivo.COCHERA, "12.5", "5000", pisoAm1),
                activo("DEPOSITO 101", TipoActivo.DEPOSITO, "8.0", "3000", pisoAm1)
        ));

        var pisoA1 = Piso.builder().nroPiso(1).torre(torreA).build();
        pisoA1.setActivos(List.of(
                activo("DEPARTAMENTO 201", TipoActivo.DEPARTAMENTO, "80.0", "80000", pisoA1),
                activo("DEPARTAMENTO 202", TipoActivo.DEPARTAMENTO, "85.0", "85000", pisoA1)
        ));

        var pisoA2 = Piso.builder().nroPiso(2).torre(torreA).build();
        pisoA2.setActivos(List.of(
                activo("DEPARTAMENTO 301", TipoActivo.DEPARTAMENTO, "80.0", "82000", pisoA2),
                activo("DEPARTAMENTO 302", TipoActivo.DEPARTAMENTO, "85.0", "87000", pisoA2)
        ));

        var pisoA3 = Piso.builder().nroPiso(3).torre(torreA).build();
        pisoA3.setActivos(List.of(
                activo("DEPARTAMENTO 401", TipoActivo.DEPARTAMENTO, "90.0", "92000", pisoA3),
                activo("DEPARTAMENTO 402", TipoActivo.DEPARTAMENTO, "95.0", "97000", pisoA3)
        ));

        var pisoA4 = Piso.builder().nroPiso(4).torre(torreA).build();
        pisoA4.setActivos(List.of(
                activo("DEPARTAMENTO 501", TipoActivo.DEPARTAMENTO, "100.0", "100000", pisoA4),
                activo("DEPARTAMENTO 502", TipoActivo.DEPARTAMENTO, "105.0", "105000", pisoA4)
        ));

        torreA.setPisos(List.of(pisoAm1, pisoA1, pisoA2, pisoA3, pisoA4));

        var torreB = Torre.builder().nombre("Torre B").proyecto(proyecto).build();

        var pisoBm1 = Piso.builder().nroPiso(-1).torre(torreB).build();
        pisoBm1.setActivos(List.of(
                activo("COCHERA 3", TipoActivo.COCHERA, "12.5", "5000", pisoBm1),
                activo("COCHERA 4", TipoActivo.COCHERA, "12.5", "5000", pisoBm1),
                activo("COCHERA 5", TipoActivo.COCHERA, "12.5", "5000", pisoBm1),
                activo("DEPOSITO 102", TipoActivo.DEPOSITO, "8.0", "3000", pisoBm1)
        ));

        var pisoB1 = Piso.builder().nroPiso(1).torre(torreB).build();
        pisoB1.setActivos(List.of(
                activo("DEPARTAMENTO 601", TipoActivo.DEPARTAMENTO, "75.0", "75000", pisoB1),
                activo("DEPARTAMENTO 602", TipoActivo.DEPARTAMENTO, "78.0", "78000", pisoB1)
        ));

        var pisoB2 = Piso.builder().nroPiso(2).torre(torreB).build();
        pisoB2.setActivos(List.of(
                activo("DEPARTAMENTO 701", TipoActivo.DEPARTAMENTO, "82.0", "82000", pisoB2),
                activo("DEPARTAMENTO 702", TipoActivo.DEPARTAMENTO, "85.0", "85000", pisoB2)
        ));

        var pisoB3 = Piso.builder().nroPiso(3).torre(torreB).build();
        pisoB3.setActivos(List.of(
                activo("DEPARTAMENTO 801", TipoActivo.DEPARTAMENTO, "95.0", "95000", pisoB3),
                activo("DEPARTAMENTO 802", TipoActivo.DEPARTAMENTO, "100.0", "100000", pisoB3)
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

    private Activo activo(String nro, TipoActivo tipo, String areaM2, String precio, Piso piso) {
        var a = Activo.builder()
                .nro(nro)
                .tipo(tipo)
                .areaM2(new BigDecimal(areaM2))
                .precio(new BigDecimal(precio))
                .estadoComercial(EstadoComercialActivo.DISPONIBLE)
                .descripcion(nro + " - " + areaM2 + " m²")
                .piso(piso)
                .build();
        return a;
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

    private UsuarioActivo asignarActivoACliente(Usuario cliente, Activo activo) {
        activo.setEstadoComercial(EstadoComercialActivo.SEPARADO);

        var ua = UsuarioActivo.builder()
                .activo(activo)
                .tipoFinanciamiento("Crédito Hipotecario")
                .faseComercial("Separación")
                .estadoTramiteLegal("Minuta Pendiente")
                .fechaAdquisicion(LocalDateTime.now())
                .build();
        ua.setClientes(List.of(cliente));
        usuarioActivoRepository.save(ua);
        log.info("Activo {} asignado al cliente {}", activo.getNro(), cliente.getEmail());
        return ua;
    }

    private void crearHitosProcesoCompra(UsuarioActivo ua) {
        var hitos = List.of(
                hitoCompra(ua, EtapaProceso.SEPARACION, 1, "Firma de Separación", EstadoHitoComercial.COMPLETADO),
                hitoCompra(ua, EtapaProceso.SEPARACION, 2, "Pago de Separación", EstadoHitoComercial.COMPLETADO),
                hitoCompra(ua, EtapaProceso.CONTRATO, 3, "Revisión de Contrato", EstadoHitoComercial.EN_PROGRESO),
                hitoCompra(ua, EtapaProceso.CONTRATO, 4, "Firma de Contrato", EstadoHitoComercial.PENDIENTE),
                hitoCompra(ua, EtapaProceso.PAGO, 5, "Evaluación Crediticia", EstadoHitoComercial.PENDIENTE),
                hitoCompra(ua, EtapaProceso.PAGO, 6, "Desembolso", EstadoHitoComercial.PENDIENTE),
                hitoCompra(ua, EtapaProceso.ENTREGA, 7, "Coordinación de Entrega", EstadoHitoComercial.PENDIENTE),
                hitoCompra(ua, EtapaProceso.ENTREGA, 8, "Acta de Entrega", EstadoHitoComercial.PENDIENTE),
                hitoCompra(ua, EtapaProceso.SANEAMIENTO, 9, "Trámite de Saneamiento", EstadoHitoComercial.PENDIENTE)
        );
        hitoProcesoCompraRepository.saveAll(hitos);
        log.info("{} hitos de proceso de compra creados", hitos.size());
    }

    private HitoProcesoCompra hitoCompra(UsuarioActivo ua, EtapaProceso etapa, int orden,
                                         String nombre, EstadoHitoComercial estado) {
        return HitoProcesoCompra.builder()
                .usuarioActivo(ua)
                .etapaProceso(etapa)
                .orden(orden)
                .nombreHito(nombre)
                .estado(estado)
                .build();
    }
}
