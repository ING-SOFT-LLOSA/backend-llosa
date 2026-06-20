package com.llosa.backend.seguridad.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.llosa.backend.config.TestData;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CP08 (Ampliado): Verificar que al desactivar un usuario
 * se invaliden sus tokens JWT y se cierren sus sesiones
 *
 * Pruebas unitarias para invalidación de tokens y cierre de sesiones.
 */
@ExtendWith(MockitoExtension.class)
class TokenInvalidationUnitTest {

    @Mock
    UsuarioRepository usuarioRepository;

    @InjectMocks
    UsuarioService usuarioService;

    // ── CP08: Desactivación invalidando tokens ────────────────────────────────────

    @Test
    void cambiarEstado_false_revocaTokensEnFirebase() throws Exception {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(100);
        usuario.setFirebaseUuid("firebase-uid-empleado");
        usuario.setActivo(true);

        when(usuarioRepository.findById(100)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        // When
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(100, false);

            // Then
            verify(mockAuth).revokeRefreshTokens("firebase-uid-empleado");
            assertThat(usuario.getActivo()).isFalse();
        }
    }

    @Test
    void cambiarEstado_false_deshabilitaEnFirebase() throws Exception {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(101);
        usuario.setFirebaseUuid("firebase-uid-empleado-2");
        usuario.setActivo(true);

        when(usuarioRepository.findById(101)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        // When
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(101, false);

            // Then
            verify(mockAuth).updateUser(any(UserRecord.UpdateRequest.class));
            assertThat(usuario.getActivo()).isFalse();
        }
    }

    @Test
    void cambiarEstado_false_marcaBDComoInactivo() throws Exception {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(102);
        usuario.setActivo(true);

        when(usuarioRepository.findById(102)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        // When
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(102, false);

            // Then
            assertThat(usuario.getActivo()).isFalse();
            verify(usuarioRepository).save(usuario);
        }
    }

    @Test
    void cambiarEstado_true_reactivaEnFirebase() throws Exception {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(103);
        usuario.setFirebaseUuid("firebase-uid-empleado-3");
        usuario.setActivo(false); // Actualmente inactivo

        when(usuarioRepository.findById(103)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        // When
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(103, true);

            // Then
            verify(mockAuth).revokeRefreshTokens("firebase-uid-empleado-3");
            verify(mockAuth).updateUser(any(UserRecord.UpdateRequest.class));
            assertThat(usuario.getActivo()).isTrue();
        }
    }

    @Test
    void cambiarEstado_true_marcaBDComoActivo() throws Exception {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(104);
        usuario.setActivo(false);

        when(usuarioRepository.findById(104)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        // When
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(104, true);

            // Then
            assertThat(usuario.getActivo()).isTrue();
            verify(usuarioRepository).save(usuario);
        }
    }

    // ── CP08 Ampliado: Escenarios de error y recuperación ──────────────────────

    @Test
    void cambiarEstado_firebaseFalla_noActualizaBD() throws Exception {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(105);
        usuario.setFirebaseUuid("firebase-uid-empleado-4");
        usuario.setActivo(true);

        when(usuarioRepository.findById(105)).thenReturn(Optional.of(usuario));

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        doThrow(new RuntimeException("Firebase timeout"))
                .when(mockAuth).revokeRefreshTokens(any());

        // When/Then
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            assertThatThrownBy(() -> usuarioService.cambiarEstado(105, false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Firebase");

            verify(usuarioRepository, never()).save(usuario);
            assertThat(usuario.getActivo()).isTrue(); // No cambia
        }
    }

    @Test
    void cambiarEstado_usuarioNoExiste_lanzaExcepcion() {
        // Given
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        // When/Then
        try (var ms = mockStatic(FirebaseAuth.class)) {
            assertThatThrownBy(() -> usuarioService.cambiarEstado(999, false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no encontrado");

            verify(usuarioRepository, never()).save(any());
        }
    }

    @Test
    void cambiarEstado_enCadena_multiplosUsuarios() throws Exception {
        // Given
        Usuario usuario1 = TestData.usuario();
        usuario1.setId(106);
        Usuario usuario2 = TestData.usuario();
        usuario2.setId(107);

        when(usuarioRepository.findById(106)).thenReturn(Optional.of(usuario1));
        when(usuarioRepository.findById(107)).thenReturn(Optional.of(usuario2));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        // When
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(106, false);
            usuarioService.cambiarEstado(107, false);

            // Then
            assertThat(usuario1.getActivo()).isFalse();
            assertThat(usuario2.getActivo()).isFalse();
            verify(mockAuth, times(2)).revokeRefreshTokens(any());
        }
    }

    @Test
    void cambiarEstado_verificaTransaccionalidad() throws Exception {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(108);
        usuario.setActivo(true);

        when(usuarioRepository.findById(108)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        // When
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(108, false);

            // Then - Verificar que se llama en el orden correcto
            InOrder inOrder = inOrder(mockAuth, usuarioRepository);
            inOrder.verify(mockAuth).revokeRefreshTokens(any());
            inOrder.verify(mockAuth).updateUser(any(UserRecord.UpdateRequest.class));
            inOrder.verify(usuarioRepository).save(usuario);
        }
    }

    @Test
    void cambiarEstado_sessionActualizaEnProximaRequest() throws Exception {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(109);
        usuario.setActivo(false); // Previamente desactivado

        when(usuarioRepository.findById(109)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        // When - Reactivar
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(109, true);

            // Then - Tokens deben ser revocados antes de reactivar
            verify(mockAuth).revokeRefreshTokens(usuario.getFirebaseUuid());
        }
    }

    @Test
    void cambiarEstado_sincronizacionFirebasePostgreSQL() throws Exception {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(110);
        usuario.setActivo(true);

        when(usuarioRepository.findById(110)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(inv -> {
            return (Usuario) inv.getArgument(0);
        });

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        // When
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(110, false);

            // Then - Ambos sistemas deben estar sincronizados
            assertThat(usuario.getActivo()).isFalse();
            verify(mockAuth).updateUser(any(UserRecord.UpdateRequest.class));
            verify(usuarioRepository).save(usuario);
        }
    }

    @Test
    void cambiarEstado_idempotencia_multiplesLlamadas() throws Exception {
        // Given
        Usuario usuario = TestData.usuario();
        usuario.setId(111);
        usuario.setActivo(true);

        when(usuarioRepository.findById(111)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);

        // When - Llamar 2 veces con mismo resultado
        try (var ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            usuarioService.cambiarEstado(111, false);
            usuarioService.cambiarEstado(111, false);

            // Then - Ambas llamadas deben ejecutarse (sin optimización de idempotencia)
            verify(mockAuth, times(2)).revokeRefreshTokens(usuario.getFirebaseUuid());
            verify(usuarioRepository, times(2)).save(usuario);
        }
    }
}
