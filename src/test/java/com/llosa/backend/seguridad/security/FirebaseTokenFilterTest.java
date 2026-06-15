package com.llosa.backend.seguridad.security;
 
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.authentication.BadCredentialsException;
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
    void tokenFirebaseInvalido_limpiaContextoYRetorna401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-invalido");

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("token-invalido"))
                .thenThrow(new BadCredentialsException("Token expirado"));

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            filter.doFilterInternal(request, response, filterChain);
        }

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
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
    void bearerConTokenVacio_limpiaContextoYRetorna401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("")).thenThrow(new BadCredentialsException("Token vacío inválido"));

        try (MockedStatic<FirebaseAuth> ms = mockStatic(FirebaseAuth.class)) {
            ms.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            filter.doFilterInternal(request, response, filterChain);
        }

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
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
}
