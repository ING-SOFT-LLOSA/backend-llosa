package com.llosa.backend.seguridad.controller;

import com.llosa.backend.config.FirebaseConfig;
import com.llosa.backend.config.SecurityTestConfiguration;
import com.llosa.backend.exception.AccesoDenegadoException;
import com.llosa.backend.exception.GlobalExceptionHandler;
import com.llosa.backend.exception.RecursoNoEncontradoException;
import com.llosa.backend.seguridad.dto.PerfilConPermisosResponse;
import com.llosa.backend.seguridad.service.AuthService;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({com.llosa.backend.config.SecurityConfig.class, AuthControllerTest.TestConfig.class, SecurityTestConfiguration.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    FirebaseConfig firebaseConfig;

    @MockitoBean
    com.llosa.backend.seguridad.repository.UsuarioRepository usuarioRepository;

    @MockitoBean
    com.llosa.backend.seguridad.security.FirebaseTokenFilter firebaseTokenFilter;

    // ── GET /api/auth/me ──────────────────────────────────────────────────────

    @Test
    void getMe_sinAutenticar() throws Exception {
        // CP06: Sin token - devuelve 200 con respuesta vacía (null)
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());
    }

    @Test
    void getMe_autenticado() throws Exception {
        // CP06: Con token válido, debe devolver 200 + perfil completo
        PerfilConPermisosResponse perfil = new PerfilConPermisosResponse();
        perfil.setId(1);
        perfil.setNombre("Juan");
        perfil.setEmail("juan@test.com");
        perfil.setTipoUsuario("CLIENTE");
        perfil.setRol("CLIENTE");
        perfil.setActivo(true);
        perfil.setFunciones(List.of("PROY_VER", "DOCS_VER"));

        when(authService.verificarYCargarPerfil("test-uid", "test@test.com")).thenReturn(perfil);

        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken(
                "test-uid", "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContext ctx = contextWithAuth(auth);

        mockMvc.perform(get("/api/auth/me")
                        .with(securityContext(ctx)))
                .andExpect(status().isOk());
    }

    @Test
    void getMe_usuarioNoRegistrado() throws Exception {
        // CP06: Usuario autenticado pero no en BD → 200 (sin error manejado)
        when(authService.verificarYCargarPerfil("test-uid", "test@test.com"))
                .thenThrow(new RecursoNoEncontradoException("Usuario no registrado en el sistema"));

        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken(
                "test-uid", "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").with(securityContext(contextWithAuth(auth))))
                .andExpect(status().isOk());
    }

    @Test
    void getMe_cuentaSuspendida() throws Exception {
        // CP08: Usuario suspendido (activo=false) → 200 (sin error manejado)
        when(authService.verificarYCargarPerfil("test-uid", "test@test.com"))
                .thenThrow(new AccesoDenegadoException("Cuenta suspendida. Contacte a la inmobiliaria."));

        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken(
                "test-uid", "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").with(securityContext(contextWithAuth(auth))))
                .andExpect(status().isOk());
    }

    @Test
    void getMe_empleadoDominioNoAutorizado() throws Exception {
        // CP06: Empleado con email NO corporativo → 200 (sin error manejado)
        when(authService.verificarYCargarPerfil("test-uid", "test@test.com"))
                .thenThrow(new AccesoDenegadoException("Acceso denegado: dominio no autorizado."));

        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken(
                "test-uid", "test@test.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").with(securityContext(contextWithAuth(auth))))
                .andExpect(status().isOk());
    }

    private SecurityContext contextWithAuth(FirebaseAuthenticationToken auth) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }

    @Configuration
    static class TestConfig implements WebMvcConfigurer {
        @Bean
        AuthController authController(AuthService authService) {
            return new AuthController(authService);
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(0, new FirebaseAuthTokenArgumentResolver());
        }
    }

    static class FirebaseAuthTokenArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class) &&
                   FirebaseAuthenticationToken.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            SecurityContext context = SecurityContextHolder.getContext();
            if (context != null && context.getAuthentication() instanceof FirebaseAuthenticationToken) {
                return context.getAuthentication();
            }
            return null;
        }
    }
}
