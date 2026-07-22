package com.notebox.api.application.auth;

import java.time.Instant;

import com.notebox.api.domain.User;

/** Issues a stateless RS256 JWT for an authenticated user (OQ-08, plan D-2). */
public interface JwtIssuer {

    IssuedToken issue(User user);

    record IssuedToken(String token, Instant expiresAt) {
    }
}
