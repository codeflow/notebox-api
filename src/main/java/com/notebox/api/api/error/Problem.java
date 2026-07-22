package com.notebox.api.api.error;

/** Uniform error envelope on the wire (AD-11). {@code message} is localized per request locale (BR-08). */
public record Problem(String code, String message, String correlationId) {
}
