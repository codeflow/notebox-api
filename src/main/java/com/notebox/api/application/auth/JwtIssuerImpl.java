package com.notebox.api.application.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.domain.User;

import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Builds and signs the RS256 access token with the standard claims (sub, tenant, groups, iss, exp). */
@ApplicationScoped
public class JwtIssuerImpl implements JwtIssuer {

    private final long ttlSeconds;
    private final String issuer;

    public JwtIssuerImpl(
            @ConfigProperty(name = "notebox.jwt.ttl-seconds") long ttlSeconds,
            @ConfigProperty(name = "notebox.jwt.issuer") String issuer) {
        this.ttlSeconds = ttlSeconds;
        this.issuer = issuer;
    }

    @Override
    public IssuedToken issue(User user) {
        Instant expiresAt = Instant.now().plus(Duration.ofSeconds(ttlSeconds));
        String token = Jwt.issuer(issuer)
                .subject(user.getId().toString())
                .upn(user.getId().toString())
                .claim("tenant", user.getTenantId().toString())
                .groups(Set.of(user.getRole().name()))
                .expiresAt(expiresAt.getEpochSecond())
                .sign();
        return new IssuedToken(token, expiresAt);
    }
}
