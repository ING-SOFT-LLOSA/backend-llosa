package com.llosa.backend.config;

import com.llosa.backend.seguridad.security.FirebaseAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithFirebaseAuthSecurityContextFactory.class)
public @interface WithFirebaseAuth {
    String uid() default "test-uid";
    String email() default "test@test.com";
}

class WithFirebaseAuthSecurityContextFactory implements org.springframework.security.test.context.support.WithSecurityContextFactory<WithFirebaseAuth> {
    @Override
    public SecurityContext createSecurityContext(WithFirebaseAuth annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken(
                annotation.uid(),
                annotation.email(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        context.setAuthentication(auth);
        return context;
    }
}
