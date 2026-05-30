package com.llosa.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseAuthenticationTokenTest {

    private final FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
            "uid-123", "user@test.com",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    @Test
    void getUid_retornaUid() {
        assertThat(token.getUid()).isEqualTo("uid-123");
    }

    @Test
    void getEmail_retornaEmail() {
        assertThat(token.getEmail()).isEqualTo("user@test.com");
    }

    @Test
    void getPrincipal_retornaUid() {
        assertThat(token.getPrincipal()).isEqualTo("uid-123");
    }

    @Test
    void getCredentials_retornaNull() {
        assertThat(token.getCredentials()).isNull();
    }

    @Test
    void isAuthenticated_retornaTrue() {
        assertThat(token.isAuthenticated()).isTrue();
    }
}
