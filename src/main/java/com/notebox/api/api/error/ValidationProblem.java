package com.notebox.api.api.error;

import java.util.List;

/** Validation error envelope: the uniform {@link Problem} shape plus the per-field violations (constitution §Errors). */
public record ValidationProblem(String code, String message, String correlationId, List<Violation> violations) {
}
