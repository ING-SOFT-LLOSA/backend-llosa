package com.llosa.backend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.proyecto.dto.request.*;
import com.llosa.backend.proyecto.entity.*;
import com.llosa.backend.proyecto.enums.*;
import com.llosa.backend.proyecto.repository.ActivoRepository;
import com.llosa.backend.proyecto.service.*;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.RolRepository;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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

    private static final String EMPLEADO_EMAIL = "empleado@demo.com";
    private static final String CLIENTE_EMAIL = "cliente@demo.com";
    private static final String DEMO_PASSWORD = "Demo123!";
    private static final String PROYECTO_NOMBRE = "Edificio Los Olivos";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ProyectoService proyectoService;
    private final HitoService hitoService;
    private final UsuarioActivoService usuarioActivoService;
    private final ActivoRepository activoRepository;

    @Override
    public void run(String... args) {
        log.info(">>>> [DEMO] Verificando perfil demo...");
        if (usuarioRepository.existsByEmail(EMPLEADO_EMAIL)) {
            log.info(">>>> [DEMO] Datos demo ya existen ({} encontrado), saltando inicialización", EMPLEADO_EMAIL);
            return;
        }

        try {
            log.info(">>>> [DEMO] Iniciando carga de datos demo...");
            
            log.info(">>>> [DEMO] Buscando roles...");
            Rol rolAsesor = rolRepository.findByNombre("ASESOR")
                    .orElseThrow(() -> new RuntimeException("Rol ASESOR no encontrado"));
            Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                    .orElseThrow(() -> new RuntimeException("Rol CLIENTE no encontrado"));

            log.info(">>>> [DEMO] Creando usuarios...");
            Integer idEmpleado = createDemoUser("Carlos", "García", EMPLEADO_EMAIL, "EMPLEADO", rolAsesor);
            Integer idCliente = createDemoUser("María", "López", CLIENTE_EMAIL, "CLIENTE", rolCliente);

            log.info(">>>> [DEMO] Usuarios demo creados: empleado={}, cliente={}", idEmpleado, idCliente);

            log.info(">>>> [DEMO] Creando proyecto...");
            Proyecto proyecto = createDemoProject();
            log.info(">>>> [DEMO] Proyecto demo creado: {}", proyecto.getId());

            log.info(">>>> [DEMO] Creando estructura...");
            createDemoStructure(proyecto);
            log.info(">>>> [DEMO] Estructura física del proyecto creada");

            log.info(">>>> [DEMO] Creando hitos...");
            createDemoHitos(proyecto);
            log.info(">>>> [DEMO] Hitos del proyecto creados");

            log.info(">>>> [DEMO] Creando contrato...");
            createDemoContract(idCliente, proyecto.getId());
            log.info(">>>> [DEMO] Contrato demo creado para el cliente");

            log.info(">>>> [DEMO] Datos de demostración creados exitosamente");
        } catch (Exception e) {
            log.error(">>>> [DEMO] ERROR CRÍTICO durante la inicialización demo: {}", e.getMessage(), e);
        }
    }

    private Integer createDemoUser(String nombre, String apellidos, String email,
                                   String tipoUsuario, Rol rol) {
        if (usuarioRepository.existsByEmail(email)) {
            log.info("Usuario ya existe en DB: {}, saltando creación", email);
            return usuarioRepository.findByEmail(email).get().getId();
        }

        String firebaseUid;
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(DEMO_PASSWORD)
                    .setDisplayName(nombre + " " + apellidos);
            UserRecord record = FirebaseAuth.getInstance().createUser(request);
            firebaseUid = record.getUid();
            log.info("Usuario creado en Firebase: {}", email);
        } catch (FirebaseAuthException e) {
            log.warn("No se pudo crear usuario en Firebase ({}), usando UUID local", e.getMessage());
            firebaseUid = "demo-" + UUID.randomUUID();
        }

        Usuario usuario = new Usuario();
        usuario.setFirebaseUuid(firebaseUid);
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setApellidos(apellidos);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        log.info("Usuario guardado en DB: {} con rol {}", email, rol.getNombre());
        return usuario.getId();
    }

    private Proyecto createDemoProject() {
        Proyecto proyecto = Proyecto.builder()
                .nombre(PROYECTO_NOMBRE)
                .descripcion("Proyecto de demostración con 1 torre y 3 pisos")
                .precertificacionEdgeLeed(true)
                .departamento("Lima")
                .distrito("San Isidro")
                .direccion("Av. Conquistadores 789")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusYears(2))
                .build();
        return proyectoService.save(proyecto);
    }

    private void createDemoStructure(Proyecto proyecto) {
        ProyectoCargaDTO carga = new ProyectoCargaDTO(List.of(
                new TorreRequestDTO("Torre A", List.of(
                        new PisoRequestDTO(1, List.of(
                                new ActivoRequestDTO("101", TipoActivo.DEPARTAMENTO,
                                        BigDecimal.valueOf(85.5), EstadoComercialActivo.DISPONIBLE,
                                        BigDecimal.valueOf(250000), "Departamento 101 - 3 dormitorios"),
                                new ActivoRequestDTO("102", TipoActivo.DEPARTAMENTO,
                                        BigDecimal.valueOf(70.0), EstadoComercialActivo.DISPONIBLE,
                                        BigDecimal.valueOf(210000), "Departamento 102 - 2 dormitorios")
                        )),
                        new PisoRequestDTO(2, List.of(
                                new ActivoRequestDTO("201", TipoActivo.DEPARTAMENTO,
                                        BigDecimal.valueOf(85.5), EstadoComercialActivo.DISPONIBLE,
                                        BigDecimal.valueOf(260000), "Departamento 201 - 3 dormitorios"),
                                new ActivoRequestDTO("202", TipoActivo.DEPARTAMENTO,
                                        BigDecimal.valueOf(70.0), EstadoComercialActivo.DISPONIBLE,
                                        BigDecimal.valueOf(220000), "Departamento 202 - 2 dormitorios")
                        )),
                        new PisoRequestDTO(3, List.of(
                                new ActivoRequestDTO("301", TipoActivo.DEPARTAMENTO,
                                        BigDecimal.valueOf(100.0), EstadoComercialActivo.DISPONIBLE,
                                        BigDecimal.valueOf(300000), "Departamento 301 - 3 dormitorios + terraza"),
                                new ActivoRequestDTO("302", TipoActivo.DEPARTAMENTO,
                                        BigDecimal.valueOf(70.0), EstadoComercialActivo.DISPONIBLE,
                                        BigDecimal.valueOf(230000), "Departamento 302 - 2 dormitorios")
                        ))
                ))
        ));
        proyectoService.cargarProyecto(proyecto.getId(), carga);
    }

    private void createDemoHitos(Proyecto proyecto) {
        Hito hito1 = Hito.builder()
                .titulo("Cimentación")
                .orden(1)
                .tipo(TipoHito.OBRA)
                .estado(EstadoHito.COMPLETADO)
                .fechaCompletado(LocalDate.now().minusMonths(3))
                .build();
        hitoService.save(proyecto.getId(), hito1);

        Hito hito2 = Hito.builder()
                .titulo("Estructura")
                .orden(2)
                .tipo(TipoHito.OBRA)
                .estado(EstadoHito.EN_PROGRESO)
                .build();
        hitoService.save(proyecto.getId(), hito2);

        Hito hito3 = Hito.builder()
                .titulo("Acabados")
                .orden(3)
                .tipo(TipoHito.OBRA)
                .estado(EstadoHito.PENDIENTE)
                .build();
        hitoService.save(proyecto.getId(), hito3);

        Hito hito4 = Hito.builder()
                .titulo("Entrega")
                .orden(4)
                .tipo(TipoHito.OBRA)
                .estado(EstadoHito.PENDIENTE)
                .build();
        hitoService.save(proyecto.getId(), hito4);
    }

    private void createDemoContract(Integer idCliente, UUID idProyecto) {
        try {
            CrearContratoDTO contratoDTO = new CrearContratoDTO(
                    List.of(idCliente),
                    "Crédito Hipotecario",
                    "VENTA",
                    "PENDIENTE",
                    LocalDateTime.now()
            );
            UsuarioActivo contrato = usuarioActivoService.crearContratoBase(contratoDTO);

            List<Activo> activos = activoRepository.findByPisoTorreProyectoId(idProyecto);
            if (!activos.isEmpty()) {
                AsignarActivoDTO asignacion = new AsignarActivoDTO(
                        contrato.getUuidUsuarioActivo(),
                        List.of(activos.getFirst().getId())
                );
                usuarioActivoService.asignarActivo(asignacion);
            }
        } catch (Exception e) {
            log.warn("No se pudo crear contrato demo: {}", e.getMessage());
        }
    }
}
