package com.llosa.backend.stress;

import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.config.TestData;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.RolRepository;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@Tag("stress")
@SpringBootTest
@ActiveProfiles("test")
@Import(SecurityTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SeguridadConcurrencyTest {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    RolRepository rolRepository;

    @BeforeEach
    void limpiar() {
        usuarioRepository.deleteAll();
    }

    // ── Inserción concurrente con email duplicado ─────────────────────────────

    /**
     * 50 hilos intentan insertar el mismo email simultáneamente.
     * La restricción UNIQUE de PostgreSQL debe garantizar exactamente 1 éxito.
     */
    @Test
    void insercionConcurrenteMismoEmail_exactamenteUnaExitosa() throws InterruptedException {
        int hilos = 50;
        String emailDuplicado = "concurrente@test.com";

        CountDownLatch listo = new CountDownLatch(hilos);
        CountDownLatch inicio = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(hilos);

        AtomicInteger exitosos = new AtomicInteger(0);
        AtomicInteger fallidos = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(hilos);

        for (int i = 0; i < hilos; i++) {
            executor.submit(() -> {
                listo.countDown();
                try {
                    inicio.await();
                    Usuario u = new Usuario();
                    u.setFirebaseUuid("uid-" + UUID.randomUUID());
                    u.setNombre("Concurrent");
                    u.setApellidos("User");
                    u.setEmail(emailDuplicado);
                    u.setTipoUsuario("CLIENTE");
                    u.setActivo(true);
                    usuarioRepository.saveAndFlush(u);
                    exitosos.incrementAndGet();
                } catch (DataIntegrityViolationException | InterruptedException e) {
                    fallidos.incrementAndGet();
                } finally {
                    fin.countDown();
                }
            });
        }

        listo.await();
        inicio.countDown();
        fin.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertThat(exitosos.get()).isEqualTo(1);
        assertThat(fallidos.get()).isEqualTo(hilos - 1);
        assertThat(usuarioRepository.existsByEmail(emailDuplicado)).isTrue();
    }

    // ── Asignación concurrente de rol sobre el mismo usuario ─────────────────

    /**
     * 10 hilos asignan distinto rol al mismo usuario.
     * La BD debe quedar en un estado consistente (uno de los dos roles).
     */
    @Test
    void asignacionConcurrenteDeRol_dbConsistente() throws InterruptedException {

        // 1. Buscamos el rol, y SOLO lo creamos si no existe previamente
        Rol rolA = rolRepository.findByNombre("CLIENTE").orElseGet(() -> {
            Rol r = new Rol();
            r.setNombre("CLIENTE");
            return rolRepository.save(r);
        });

        Rol rolB = rolRepository.findByNombre("ASESOR").orElseGet(() -> {
            Rol r = new Rol();
            r.setNombre("ASESOR");
            return rolRepository.save(r);
        });

        Usuario usuario = TestData.usuarioConEmail("rol-concurrente@test.com");
        usuarioRepository.save(usuario);
        Integer uid = usuario.getId();

        int hilos = 10;
        CountDownLatch listo = new CountDownLatch(hilos);
        CountDownLatch inicio = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(hilos);

        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        // 2. Ejecutamos la prueba de concurrencia
        for (int i = 0; i < hilos; i++) {
            final Rol rol = (i % 2 == 0) ? rolA : rolB;
            executor.submit(() -> {
                listo.countDown();
                try {
                    inicio.await();
                    usuarioRepository.findById(uid).ifPresent(u -> {
                        u.setRol(rol);
                        usuarioRepository.save(u);
                    });
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    fin.countDown();
                }
            });
        }

        listo.await();
        inicio.countDown();
        fin.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();


        // 3. Verificamos consistencia
        Usuario resultado = usuarioRepository.findById(uid).orElseThrow();
        assertThat(errors).isEmpty();
        assertThat(resultado.getRol()).isNotNull();
        assertThat(resultado.getRol().getNombre()).isIn("CLIENTE", "ASESOR");
    }

    // ── RepeatedTest: sin fugas de conexiones ────────────────────────────────

    /**
     * 50 operaciones de escritura secuenciales para detectar fugas de conexión o transacciones
     * colgadas que agoten el pool.
     */
    @RepeatedTest(50)
    void guardarYBorrarUsuario_noAgotaElPool() {
        String email = "pool-test-" + UUID.randomUUID() + "@test.com";
        Usuario u = TestData.usuarioConEmail(email);
        usuarioRepository.save(u);
        usuarioRepository.delete(u);

        assertThat(usuarioRepository.existsByEmail(email)).isFalse();
    }

    // ── Lectura concurrente bajo carga ────────────────────────────────────────

    /**
     * Seed de 100 usuarios + 20 lecturas concurrentes.
     * Valida que no haya deadlocks ni inconsistencias en lectura.
     */
    @Test
    void lecturasConcurrentes_sinErrores() throws InterruptedException {
        List<Usuario> usuarios = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            usuarios.add(TestData.usuarioConEmail("read-" + i + "@test.com"));
        }
        usuarioRepository.saveAll(usuarios);

        int hilos = 20;
        CountDownLatch inicio = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(hilos);
        AtomicInteger errores = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(hilos);

        for (int i = 0; i < hilos; i++) {
            executor.submit(() -> {
                try {
                    inicio.await();
                    List<Usuario> resultado = usuarioRepository.findAll();
                    if (resultado.size() < 100) errores.incrementAndGet();
                } catch (Exception e) {
                    errores.incrementAndGet();
                } finally {
                    fin.countDown();
                }
            });
        }

        inicio.countDown();
        fin.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertThat(errores.get()).isZero();
    }
}
