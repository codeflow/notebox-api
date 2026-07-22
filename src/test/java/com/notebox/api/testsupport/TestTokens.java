package com.notebox.api.testsupport;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import io.smallrye.jwt.build.Jwt;

/** Mints RS256 JWTs (signed with the configured test key) for security tests. */
public final class TestTokens {

    private static final String ISSUER = "https://notebox.api";

    private TestTokens() {
    }

    public static String valid(UUID userId, UUID tenantId, String role) {
        return Jwt.issuer(ISSUER)
                .subject(userId.toString())
                .upn(userId.toString())
                .claim("tenant", tenantId.toString())
                .groups(Set.of(role))
                .expiresIn(Duration.ofHours(1))
                .sign();
    }

    public static String expired(UUID userId, UUID tenantId, String role) {
        return Jwt.issuer(ISSUER)
                .subject(userId.toString())
                .upn(userId.toString())
                .claim("tenant", tenantId.toString())
                .groups(Set.of(role))
                .expiresAt(Instant.now().minusSeconds(3600).getEpochSecond())
                .sign();
    }

    /** A structurally valid token whose signature does not verify. */
    public static String badSignature(UUID userId, UUID tenantId, String role) {
        String token = valid(userId, tenantId, role);
        String[] parts = token.split("\\.");
        return parts[0] + "." + parts[1] + ".AAAA" + parts[2];
    }
}
