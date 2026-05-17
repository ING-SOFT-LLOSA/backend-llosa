package com.llosa.backend.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {

    private final String uid;
    private final String email;

    public FirebaseAuthenticationToken(String uid, String email,
                                       Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.uid = uid;
        this.email = email;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() { return null; }

    @Override
    public Object getPrincipal() { return uid; }

    public String getEmail() { return email; }
    public String getUid()   { return uid; }
}