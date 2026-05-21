package com.llosa.backend.module.seguridad.service;

import com.llosa.backend.config.TestData;
import com.llosa.backend.module.seguridad.dto.PerfilConPermisosResponse;
import com.llosa.backend.module.seguridad.entity.Rol;
import com.llosa.backend.module.seguridad.entity.Usuario;
import com.llosa.backend.module.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "dominioCorporativo", "llosaedificaciones.com");
    }

    // ── Usuario no encontrado ─────────────────────────────────────────────────

    @Test
    void verificarPerfil_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findByFirebaseUuid("uid-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verificarYCargarPerfil("uid-x", "x@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no registrado");
    }

    // ── Usuario suspendido ────────────────────────────────────────────────────

    @Test
    void verificarPerfil_usuarioSuspendido_lanzaExcepcion() {
        Usuario u = TestData.usuario();
        u.setActivo(false);
        when(usuarioRepository.findByFirebaseUuid(u.getFirebaseUuid()))
                .thenReturn(Optional.of(u));

        assertThatThrownBy(() ->
                authService.verificarYCargarPerfil(u.getFirebaseUuid(), u.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("suspendida");
    }

    // ── Dominio corporativo ───────────────────────────────────────────────────

    @Test
    void verificarPerfil_empleadoConEmailNoCorporativo_lanzaExcepcion() {
        Usuario empleado = TestData.usuarioEmpleado("juan@gmail.com");
        when(usuarioRepository.findByFirebaseUuid(empleado.getFirebaseUuid()))
                .thenReturn(Optional.of(empleado));

        assertThatThrownBy(() ->
                authService.verificarYCargarPerfil(empleado.getFirebaseUuid(), "juan@gmail.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("dominio no autorizado");
    }

    @Test
    void verificarPerfil_empleadoConEmailCorporativo_devuelvePerfil() {
        String emailCorp = "juan@llosaedificaciones.com";
        Usuario empleado = TestData.usuarioEmpleado(emailCorp);
        Rol rol = TestData.rol();
        empleado.setRol(rol);
        when(usuarioRepository.findByFirebaseUuid(empleado.getFirebaseUuid()))
                .thenReturn(Optional.of(empleado));

        PerfilConPermisosResponse perfil =
                authService.verificarYCargarPerfil(empleado.getFirebaseUuid(), emailCorp);

        assertThat(perfil.getTipoUsuario()).isEqualTo("EMPLEADO");
        assertThat(perfil.getEmail()).isEqualTo(emailCorp);
        assertThat(perfil.getFunciones()).isNotEmpty();
    }

    // ── Cliente sin restricción de dominio ───────────────────────────────────

    @Test
    void verificarPerfil_clienteConEmailExterno_devuelvePerfil() {
        Usuario cliente = TestData.usuario();
        Rol rol = TestData.rol();
        cliente.setRol(rol);
        when(usuarioRepository.findByFirebaseUuid(cliente.getFirebaseUuid()))
                .thenReturn(Optional.of(cliente));

        PerfilConPermisosResponse perfil =
                authService.verificarYCargarPerfil(cliente.getFirebaseUuid(), cliente.getEmail());

        assertThat(perfil.getTipoUsuario()).isEqualTo("CLIENTE");
        assertThat(perfil.getFunciones()).containsExactlyInAnyOrder("PROY_VER", "DOCS_VER");
    }

    // ── Usuario sin rol ───────────────────────────────────────────────────────

    @Test
    void verificarPerfil_usuarioSinRol_devuelveFuncionesVacias() {
        Usuario cliente = TestData.usuario();
        cliente.setRol(null);
        when(usuarioRepository.findByFirebaseUuid(cliente.getFirebaseUuid()))
                .thenReturn(Optional.of(cliente));

        PerfilConPermisosResponse perfil =
                authService.verificarYCargarPerfil(cliente.getFirebaseUuid(), cliente.getEmail());

        assertThat(perfil.getFunciones()).isEmpty();
        assertThat(perfil.getRol()).isNull();
    }

    // ── Campos del perfil retornado ───────────────────────────────────────────

    @Test
    void verificarPerfil_mapeoCompletoDeUsuario() {
        Usuario cliente = TestData.usuario();
        cliente.setId(42);
        when(usuarioRepository.findByFirebaseUuid(cliente.getFirebaseUuid()))
                .thenReturn(Optional.of(cliente));

        PerfilConPermisosResponse perfil =
                authService.verificarYCargarPerfil(cliente.getFirebaseUuid(), cliente.getEmail());

        assertThat(perfil.getId()).isEqualTo(42);
        assertThat(perfil.getNombre()).isEqualTo("Juan");
        assertThat(perfil.getActivo()).isTrue();
    }
}
