package com.llosa.backend.seguridad.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.EmailDuplicadoException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.seguridad.dto.UsuarioResponse;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.RolRepository;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    RolRepository rolRepository;

    @InjectMocks
    UsuarioService usuarioService;

    // ── crearUsuario ─────────────────────────────────────────────────────────

    @Test
    void crearUsuario_emailDuplicado_lanzaExcepcionSinLlamarFirebase() {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(true);

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            assertThatThrownBy(() -> usuarioService.crearUsuario(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ya está registrado");

            ms.verifyNoInteractions();
        }
    }

    @Test
    void crearUsuario_emailDuplicado_lanzaEmailDuplicadoException() {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(true);

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            assertThatThrownBy(() -> usuarioService.crearUsuario(req))
                    .isInstanceOf(EmailDuplicadoException.class)
                    .hasMessageContaining(req.getEmail());

            ms.verifyNoInteractions();
        }
    }

    @Test
    void crearUsuario_exitoso_creaEnFirebaseYPersisteUid() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);

        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn("firebase-uid-generado");

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(mockRecord);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(99);
            return u;
        });

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            UsuarioResponse resultado = usuarioService.crearUsuario(req);

            assertThat(resultado.getEmail()).isEqualTo(req.getEmail());
            assertThat(captor.getValue().getFirebaseUuid()).isEqualTo("firebase-uid-generado");
            verify(mockAuth).createUser(any(UserRecord.CreateRequest.class));
        }
    }

    @Test
    void crearUsuario_conRolAsignado_persiteRelacion() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setIdRol(1);
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);

        Rol rol = TestData.rol();
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));

        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn("uid-con-rol");
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.createUser(any())).thenReturn(mockRecord);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(5);
            return u;
        });

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            UsuarioResponse resultado = usuarioService.crearUsuario(req);

            assertThat(resultado.getRol()).isEqualTo(rol.getNombre());
            assertThat(captor.getValue().getRol()).isNotNull();
        }
    }

    @Test
    void crearUsuario_conRolInexistente_lanzaExcepcion() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setIdRol(999);
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(rolRepository.findById(999)).thenReturn(Optional.empty());

        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn("uid-x");
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.createUser(any())).thenReturn(mockRecord);

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            assertThatThrownBy(() -> usuarioService.crearUsuario(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Rol no encontrado");
        }
    }

    @Test
    void crearUsuario_firebaseLanzaExcepcion_noPersiste() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.createUser(any(UserRecord.CreateRequest.class)))
                .thenThrow(new RuntimeException("Firebase no disponible"));

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            assertThatThrownBy(() -> usuarioService.crearUsuario(req))
                    .isInstanceOf(RuntimeException.class);

            verify(usuarioRepository, never()).save(any());
        }
    }

    // ── cambiarEstado ─────────────────────────────────────────────────────────

    @Test
    void cambiarEstado_false_revocaTokensDesactivaFirebaseYMarcaBD() throws Exception {
        Usuario usuario = TestData.usuario();
        usuario.setId(10);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(10, false);

            verify(mockAuth).revokeRefreshTokens(usuario.getFirebaseUuid());
            verify(mockAuth).updateUser(any(UserRecord.UpdateRequest.class));
            assertThat(usuario.getActivo()).isFalse();
        }
    }

    @Test
    void cambiarEstado_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            assertThatThrownBy(() -> usuarioService.cambiarEstado(999, false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no encontrado");
        }
    }

    // ── asignarRol ────────────────────────────────────────────────────────────

    @Test
    void asignarRol_rolInexistente_lanzaExcepcion() {
        Usuario usuario = TestData.usuario();
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.asignarRol(1, 99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol no encontrado");
    }

    @Test
    void asignarRol_exitoso_actualizaYDevuelveRespuesta() {
        Usuario usuario = TestData.usuario();
        Rol rol = TestData.rol();
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        UsuarioResponse resultado = usuarioService.asignarRol(1, 1);

        assertThat(resultado.getRol()).isEqualTo(rol.getNombre());
        assertThat(resultado.getFunciones())
                .containsExactlyInAnyOrder("PROY_VER", "DOCS_VER");
    }

    @Test
    void cambiarEstado_firebaseLanzaExcepcionEnRevoke_noActualizaBD() throws Exception {
        Usuario usuario = TestData.usuario();
        usuario.setId(10);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        doThrow(new RuntimeException("Firebase no disponible"))
                .when(mockAuth).revokeRefreshTokens(usuario.getFirebaseUuid());

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            assertThatThrownBy(() -> usuarioService.cambiarEstado(10, false))
                    .isInstanceOf(RuntimeException.class);

            verify(usuarioRepository, never()).save(any());
            assertThat(usuario.getActivo()).isTrue();
        }
    }

    @Test
    void cambiarEstado_true_reactivaUsuarioEnFirebaseYBD() throws Exception {
        Usuario usuario = TestData.usuario();
        usuario.setId(10);
        usuario.setActivo(false);
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(10, true);

            verify(mockAuth).revokeRefreshTokens(usuario.getFirebaseUuid());
            verify(mockAuth).updateUser(any(UserRecord.UpdateRequest.class));
            assertThat(usuario.getActivo()).isTrue();
        }
    }

    @Test
    void cambiarEstado_usuarioNoEncontrado_lanzaRecursoNoEncontrado() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            assertThatThrownBy(() -> usuarioService.cambiarEstado(999, false))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("no encontrado");
        }
    }

    @Test
    void asignarRol_usuarioNoEncontrado_lanzaRecursoNoEncontrado() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.asignarRol(999, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    void crearUsuario_sinRol_persisteConActivoTrue() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setIdRol(null);
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);

        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn("uid-sin-rol");
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.createUser(any())).thenReturn(mockRecord);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(7);
            return u;
        });

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            UsuarioResponse resultado = usuarioService.crearUsuario(req);

            assertThat(resultado.getRol()).isNull();
            assertThat(captor.getValue().getActivo()).isTrue();
            assertThat(captor.getValue().getRol()).isNull();
        }
    }

    // ── listarTodos ───────────────────────────────────────────────────────────

    @Test
    void listarTodos_devuelveTodosLosUsuarios() {
        when(usuarioRepository.findAll())
                .thenReturn(java.util.List.of(TestData.usuario(), TestData.usuario()));

        assertThat(usuarioService.listarTodos()).hasSize(2);
    }

    @Test
    void listarTodos_sinUsuarios_devuelveListaVacia() {
        when(usuarioRepository.findAll()).thenReturn(java.util.List.of());

        assertThat(usuarioService.listarTodos()).isEmpty();
    }
}
