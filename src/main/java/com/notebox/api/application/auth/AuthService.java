package com.notebox.api.application.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.notebox.api.api.error.ApiException;
import com.notebox.api.domain.User;
import com.notebox.api.infrastructure.persistence.CredentialLookup;

/** Authenticates credentials and issues a token. Invalid credentials never disclose which part failed. */
@ApplicationScoped
public class AuthService {

    private final CredentialLookup credentialLookup;
    private final JwtIssuer jwtIssuer;
    private final PasswordHasher passwordHasher;

    public AuthService(CredentialLookup credentialLookup, JwtIssuer jwtIssuer, PasswordHasher passwordHasher) {
        this.credentialLookup = credentialLookup;
        this.jwtIssuer = jwtIssuer;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public JwtIssuer.IssuedToken authenticate(String email, String password) {
        User user = credentialLookup.findByEmail(email)
                .filter(User::isActive)
                .filter(candidate -> passwordHasher.verify(password, candidate.getPasswordHash()))
                .orElseThrow(ApiException::invalidCredentials);
        return jwtIssuer.issue(user);
    }
}
