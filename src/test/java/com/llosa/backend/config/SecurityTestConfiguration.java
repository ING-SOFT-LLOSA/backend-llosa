package com.llosa.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import com.llosa.backend.seguridad.security.FirebaseTokenFilter;
import com.llosa.backend.seguridad.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class SecurityTestConfiguration implements WebMvcConfigurer {

    @Bean(name = "mockGoogleCloudStorage")
    @Primary
    public Storage googleCloudStorage() {
        return mock(Storage.class);
    }

    @Bean(name = "mockFirebaseTokenFilter")
    @Primary
    public FirebaseTokenFilter firebaseTokenFilter() {
        return new FirebaseTokenFilter(mock(UsuarioRepository.class)) {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean(name = "gcsBucketName")
    public String gcsBucketName() {
        return "bucket-falso-de-prueba";
    }

    @Bean(name = "mockFirebaseConfig")
    @Primary
    public FirebaseConfig firebaseConfig() {
        return mock(FirebaseConfig.class);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public HandlerMethodArgumentResolver authenticationPrincipalResolver() {
        return new FirebaseAuthTokenArgumentResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(0, new FirebaseAuthTokenArgumentResolver());
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
            var context = SecurityContextHolder.getContext();
            if (context != null && context.getAuthentication() instanceof FirebaseAuthenticationToken) {
                return context.getAuthentication();
            }
            return null;
        }
    }
}
