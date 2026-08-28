package com.notebox.api.api.error;

import java.util.List;

/**
 * Validation error envelope: the uniform {@link Problem} shape plus the per-field violations
 * (constitution §Errors).
 *
 * @param code the message key identifying the failure
 * @param message the key resolved to the request's locale
 * @param correlationId the id tying this response to the server logs
 * @param violations one entry per field that failed, in no guaranteed order
 */
public record ValidationProblem(String code, String message, String correlationId, List<Violation> violations) {
}
