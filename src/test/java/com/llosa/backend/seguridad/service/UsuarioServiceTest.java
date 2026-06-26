package com.llosa.backend.seguridad.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.config.TestData;
import com.llosa.backend.exception.EmailDuplicadoException;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.dto.CrearUsuarioRequest;
import com.llosa.backend.seguridad.dto.UsuarioResponse;
import com.llosa.backend.seguridad.entity.Funcion;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.RolRepository;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    RolRepository rolRepository;

    @Mock
    com.llosa.backend.seguridad.repository.FuncionRepository funcionRepository;

    @InjectMocks
    UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(usuarioService, "dominioCorporativo", "test.com");
    }

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
        req.setTipoUsuario("EMPLEADO");
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
        req.setTipoUsuario("EMPLEADO");
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
        req.setTipoUsuario("EMPLEADO");
        req.setEmail("empleado@test.com");
        req.setIdRol(999);
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(rolRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.crearUsuario(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    void crearUsuario_firebaseLanzaExcepcion_noPersiste() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setEmail("cliente@test.com");
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.crearUsuario(req))
                .isInstanceOf(RuntimeException.class);
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
    void asignarRol_exitoso_actualizaYDevuelveRespuesta() throws Exception {
        Usuario usuario = TestData.usuario();
        Rol rol = TestData.rol();
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            UsuarioResponse resultado = usuarioService.asignarRol(1, 1);

            assertThat(resultado.getRol()).isEqualTo(rol.getNombre());
            assertThat(resultado.getFunciones())
                    .containsExactlyInAnyOrder("PROY_VER", "DOCS_VER");
        }
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
        req.setTipoUsuario("EMPLEADO");
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

    // ── crearUsuario: validación de dominio corporativo / rol CLIENTE ────────

    @Test
    void crearUsuario_empleadoConDominioIncorrecto_lanzaBusinessException() {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setTipoUsuario("EMPLEADO");
        req.setEmail("ana.garcia@gmail.com");
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            assertThatThrownBy(() -> usuarioService.crearUsuario(req))
                    .isInstanceOf(com.llosa.backend.exception.BusinessException.class)
                    .hasMessageContaining("dominio corporativo");

            ms.verifyNoInteractions();
            verify(usuarioRepository, never()).save(any());
        }
    }

    @Test
    void crearUsuario_empleadoConDominioCorrecto_noLanzaExcepcionDeDominio() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setTipoUsuario("EMPLEADO");
        req.setEmail("empleado@test.com");
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);

        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn("uid-empleado-dominio-ok");
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.createUser(any())).thenReturn(mockRecord);
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            UsuarioResponse resultado = usuarioService.crearUsuario(req);

            assertThat(resultado.getEmail()).isEqualTo("empleado@test.com");
        }
    }

    @Test
    void crearUsuario_cliente_asignaRolClienteAutomaticamente() throws Exception {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setTipoUsuario("CLIENTE");
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);

        Rol rolCliente = TestData.rol();
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(rolCliente));

        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn("uid-cliente");
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.createUser(any())).thenReturn(mockRecord);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.crearUsuario(req);

            assertThat(captor.getValue().getRol()).isEqualTo(rolCliente);
        }
    }

    @Test
    void crearUsuario_cliente_sinRolEnBD_lanzaRecursoNoEncontrado() {
        CrearUsuarioRequest req = TestData.crearUsuarioRequest();
        req.setTipoUsuario("CLIENTE");
        when(usuarioRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.empty());

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            assertThatThrownBy(() -> usuarioService.crearUsuario(req))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Rol CLIENTE no encontrado");

            ms.verifyNoInteractions();
        }
    }

    // ── eliminarCompletamente ─────────────────────────────────────────────────

    @Test
    void eliminarCompletamente_exitoso_borraEnFirebaseYBD() throws Exception {
        Usuario usuario = TestData.usuario();
        usuario.setId(15);
        when(usuarioRepository.findById(15)).thenReturn(Optional.of(usuario));

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.eliminarCompletamente(15);

            verify(mockAuth).deleteUser(usuario.getFirebaseUuid());
            verify(usuarioRepository).delete(usuario);
        }
    }

    @Test
    void eliminarCompletamente_usuarioNoEncontrado_lanzaExcepcion() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            assertThatThrownBy(() -> usuarioService.eliminarCompletamente(999))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            ms.verifyNoInteractions();
            verify(usuarioRepository, never()).delete(any());
        }
    }

    // ── findById / findByFirebaseUuid ─────────────────────────────────────────

    @Test
    void findById_existente_retornaUsuario() {
        Usuario usuario = TestData.usuario();
        usuario.setId(3);
        when(usuarioRepository.findById(3)).thenReturn(Optional.of(usuario));

        assertThat(usuarioService.findById(3)).isEqualTo(usuario);
    }

    @Test
    void findById_noExistente_lanzaExcepcion() {
        when(usuarioRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.findById(404))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void findByFirebaseUuid_existente_retornaUsuario() {
        Usuario usuario = TestData.usuario();
        when(usuarioRepository.findByFirebaseUuid("uid-1")).thenReturn(Optional.of(usuario));

        assertThat(usuarioService.findByFirebaseUuid("uid-1")).isEqualTo(usuario);
    }

    @Test
    void findByFirebaseUuid_noExistente_lanzaExcepcion() {
        when(usuarioRepository.findByFirebaseUuid("uid-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.findByFirebaseUuid("uid-inexistente"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ── listarPaginadoYFiltrado ────────────────────────────────────────────────

    @Test
    void listarPaginadoYFiltrado_devuelvePaginaMapeada() {
        Usuario usuario = TestData.usuario();
        org.springframework.data.domain.Page<Usuario> pagina =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(usuario));
        when(usuarioRepository.buscarUsuariosPaginados(eq("juan"), any())).thenReturn(pagina);

        var resultado = usuarioService.listarPaginadoYFiltrado("juan", 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).email()).isEqualTo(usuario.getEmail());
    }

    // ── actualizarUsuario ──────────────────────────────────────────────────────

    @Test
    void actualizarUsuario_conTodosLosCampos_actualizaTodo() {
        Usuario usuario = TestData.usuario();
        usuario.setId(20);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        com.llosa.backend.seguridad.dto.UpdateUsuarioDTO dto = new com.llosa.backend.seguridad.dto.UpdateUsuarioDTO();
        dto.setNombre("NuevoNombre");
        dto.setApellidos("NuevoApellido");
        dto.setTelefono("999888777");
        dto.setEmail("nuevo@test.com");
        dto.setDocumentoIdentidad("87654321");
        dto.setTipoUsuario("EMPLEADO");

        UsuarioResponse resultado = usuarioService.actualizarUsuario(20, dto);

        assertThat(resultado.getNombre()).isEqualTo("NuevoNombre");
        assertThat(usuario.getApellidos()).isEqualTo("NuevoApellido");
        assertThat(usuario.getTelefono()).isEqualTo("999888777");
        assertThat(usuario.getEmail()).isEqualTo("nuevo@test.com");
        assertThat(usuario.getDocumentoIdentidad()).isEqualTo("87654321");
        assertThat(usuario.getTipoUsuario()).isEqualTo("EMPLEADO");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizarUsuario_conCamposNulos_noModificaEsosCampos() {
        Usuario usuario = TestData.usuario();
        usuario.setId(21);
        String nombreOriginal = usuario.getNombre();
        String emailOriginal = usuario.getEmail();
        when(usuarioRepository.findById(21)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        com.llosa.backend.seguridad.dto.UpdateUsuarioDTO dto = new com.llosa.backend.seguridad.dto.UpdateUsuarioDTO();

        usuarioService.actualizarUsuario(21, dto);

        assertThat(usuario.getNombre()).isEqualTo(nombreOriginal);
        assertThat(usuario.getEmail()).isEqualTo(emailOriginal);
    }

    @Test
    void actualizarUsuario_noEncontrado_lanzaExcepcion() {
        when(usuarioRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizarUsuario(404, new com.llosa.backend.seguridad.dto.UpdateUsuarioDTO()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ── modificarFunciones ─────────────────────────────────────────────────────

    @Test
    void modificarFunciones_exitoso_actualizaListaDeFunciones() {
        Usuario usuario = TestData.usuario();
        usuario.setId(30);
        Rol rol = TestData.rol();
        usuario.setRol(rol);
        when(usuarioRepository.findById(30)).thenReturn(Optional.of(usuario));

        Funcion f1 = TestData.funcion("PROY_VER");
        Funcion f2 = TestData.funcion("DOCS_SUBIR");
        when(funcionRepository.findAllById(java.util.List.of(1, 2))).thenReturn(java.util.List.of(f1, f2));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        usuarioService.modificarFunciones(30, java.util.List.of(1, 2));

        assertThat(rol.getFunciones()).containsExactly(f1, f2);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void modificarFunciones_usuarioSinRol_lanzaExcepcion() {
        Usuario usuario = TestData.usuario();
        usuario.setId(31);
        usuario.setRol(null);
        when(usuarioRepository.findById(31)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.modificarFunciones(31, java.util.List.of(1)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no tiene un rol asignado");
    }

    @Test
    void modificarFunciones_funcionesInexistentes_lanzaExcepcion() {
        Usuario usuario = TestData.usuario();
        usuario.setId(32);
        usuario.setRol(TestData.rol());
        when(usuarioRepository.findById(32)).thenReturn(Optional.of(usuario));
        when(funcionRepository.findAllById(java.util.List.of(1, 2, 3)))
                .thenReturn(java.util.List.of(TestData.funcion("PROY_VER")));

        assertThatThrownBy(() -> usuarioService.modificarFunciones(32, java.util.List.of(1, 2, 3)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existen");
    }

    @Test
    void modificarFunciones_usuarioNoEncontrado_lanzaExcepcion() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.modificarFunciones(999, java.util.List.of(1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
