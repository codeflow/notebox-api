package com.notebox.api.api.dto;

import java.time.Instant;

/** Issued token and its expiry. */
public record LoginResponse(String token, Instant expiresAt) {
}
