package com.llosa.backend.config;

import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@TestConfiguration
public class SecurityTestConfiguration implements WebMvcConfigurer {

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
