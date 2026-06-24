package com.llosa.backend.seguridad.security;
 
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.llosa.backend.seguridad.entity.Usuario;
import com.llosa.backend.seguridad.entity.Rol;
import com.llosa.backend.seguridad.entity.Funcion;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
 
import java.util.List;
import java.util.Optional;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
class FirebaseTokenFilterTest {
 
    private FirebaseTokenFilter filter;
    private UsuarioRepository usuarioRepository;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
 
    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        filter = new FirebaseTokenFilter(usuarioRepository);
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }
 
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
 
    // ── Sin header Authorization ─────────────────────────────────────────────
 
    @Test
    void sinHeaderAuthorization_continuaEncadenadoSinAutenticar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
 
        filter.doFilterInternal(request, response, filterChain);
 
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
 
    // ── Header sin "Bearer " ──────────────────────────────────────────────────
 
    @Test
    void headerBasic_sinBearer_ignoraYContinua() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
 
        filter.doFilterInternal(request, response, filterChain);
 
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
 
    // ── Token inválido ────────────────────────────────────────────────────────
 
    @Test
    void tokenFirebaseInvalido_limpiaContextoYContinua() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-invalido");
 
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("token-invalido"))
                .thenThrow(new RuntimeException("Token expirado"));
 
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
 
            filter.doFilterInternal(request, response, filterChain);
        }
 
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
 
    // ── Token válido ──────────────────────────────────────────────────────────
 
    @Test
    void tokenValido_autenticaEnSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");
 
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("uid-test-123");
        when(mockToken.getEmail()).thenReturn("user@test.com");
 
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("token-valido")).thenReturn(mockToken);
 
        Usuario mockUsuario = new Usuario();
        mockUsuario.setFirebaseUuid("uid-test-123");
        Rol mockRol = new Rol();
        Funcion mockFuncion = new Funcion();
        mockFuncion.setNombreCodigo("ROLE_USER");
        mockRol.setFunciones(List.of(mockFuncion));
        mockUsuario.setRol(mockRol);
        when(usuarioRepository.findByFirebaseUuid("uid-test-123")).thenReturn(Optional.of(mockUsuario));
 
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
 
            filter.doFilterInternal(request, response, filterChain);
        }
 
        verify(filterChain).doFilter(request, response);
 
        FirebaseAuthenticationToken auth =
                (FirebaseAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
 
        assertThat(auth).isNotNull();
        assertThat(auth.getUid()).isEqualTo("uid-test-123");
        assertThat(auth.getEmail()).isEqualTo("user@test.com");
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }
 
    @Test
    void tokenValido_siempreContinuaLaCadena() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-ok");
 
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("uid-ok");
        when(mockToken.getEmail()).thenReturn("ok@test.com");
 
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("token-ok")).thenReturn(mockToken);
 
        Usuario mockUsuario = new Usuario();
        mockUsuario.setFirebaseUuid("uid-ok");
        when(usuarioRepository.findByFirebaseUuid("uid-ok")).thenReturn(Optional.of(mockUsuario));
 
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            filter.doFilterInternal(request, response, filterChain);
        }
 
        // La cadena continúa independientemente del resultado del token
        verify(filterChain, times(1)).doFilter(request, response);
    }
 
    // ── "Bearer " sin token — pasa el startsWith pero token es vacío ─────────
 
    @Test
    void bearerConTokenVacio_limpiaContextoYContinua() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
 
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("")).thenThrow(new RuntimeException("Token vacío inválido"));
 
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            filter.doFilterInternal(request, response, filterChain);
        }
 
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
 
    // ── Token válido sobreescribe autenticación preexistente ─────────────────
 
    @Test
    void tokenValido_sobreescribeContextoPrevio() throws Exception {
        FirebaseAuthenticationToken prevAuth = new FirebaseAuthenticationToken(
                "uid-previo", "previo@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(prevAuth);
 
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer nuevo-token");
 
        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("uid-nuevo");
        when(mockToken.getEmail()).thenReturn("nuevo@test.com");
 
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("nuevo-token")).thenReturn(mockToken);
 
        Usuario mockUsuario = new Usuario();
        mockUsuario.setFirebaseUuid("uid-nuevo");
        Rol mockRol = new Rol();
        Funcion mockFuncion = new Funcion();
        mockFuncion.setNombreCodigo("ROLE_USER");
        mockRol.setFunciones(List.of(mockFuncion));
        mockUsuario.setRol(mockRol);
        when(usuarioRepository.findByFirebaseUuid("uid-nuevo")).thenReturn(Optional.of(mockUsuario));
 
        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            filter.doFilterInternal(request, response, filterChain);
        }
 
        FirebaseAuthenticationToken auth =
                (FirebaseAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getUid()).isEqualTo("uid-nuevo");
        assertThat(auth.getEmail()).isEqualTo("nuevo@test.com");
    }

    // ── Usuario suspendido (activo = false) ──────────────────────────────────

    @Test
    void usuarioSuspendido_limpiaContextoYContinua() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-suspendido");

        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("uid-susp");
        when(mockToken.getEmail()).thenReturn("susp@test.com");

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("token-suspendido")).thenReturn(mockToken);

        Usuario suspendido = new Usuario();
        suspendido.setFirebaseUuid("uid-susp");
        suspendido.setActivo(false); // -> BadCredentialsException "Usuario suspendido"
        when(usuarioRepository.findByFirebaseUuid("uid-susp")).thenReturn(Optional.of(suspendido));

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── Usuario verificado en Firebase pero no existe en BD ──────────────────

    @Test
    void usuarioNoExisteEnBD_limpiaContextoYContinua() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-sin-usuario");

        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("uid-fantasma");
        when(mockToken.getEmail()).thenReturn("fantasma@test.com");

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("token-sin-usuario")).thenReturn(mockToken);
        when(usuarioRepository.findByFirebaseUuid("uid-fantasma")).thenReturn(Optional.empty());

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── Error crítico inesperado -> HTTP 500 (manejarErrorCritico) ───────────

    @Test
    void errorRuntimeInesperado_devuelve500() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-explota");

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        // RuntimeException SIN "Token"/"expirado" en el mensaje -> manejarErrorCritico (500)
        when(mockAuth.verifyIdToken("token-explota"))
                .thenThrow(new RuntimeException("NullPointer inesperado en parsing"));

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            filter.doFilterInternal(request, response, filterChain);
        }

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("Internal server error");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }

    // ── shouldNotFilter: rutas públicas (swagger / api-docs) ─────────────────

    @Test
    void shouldNotFilter_rutasPublicas_true() throws Exception {
        var req1 = new MockHttpServletRequest(); req1.setRequestURI("/v3/api-docs");
        var req2 = new MockHttpServletRequest(); req2.setRequestURI("/swagger-ui/index.html");
        var req3 = new MockHttpServletRequest(); req3.setRequestURI("/swagger-ui.html");
        var reqProtegida = new MockHttpServletRequest(); reqProtegida.setRequestURI("/api/users");

        assertThat(filter.shouldNotFilter(req1)).isTrue();
        assertThat(filter.shouldNotFilter(req2)).isTrue();
        assertThat(filter.shouldNotFilter(req3)).isTrue();
        assertThat(filter.shouldNotFilter(reqProtegida)).isFalse();
    }
}
