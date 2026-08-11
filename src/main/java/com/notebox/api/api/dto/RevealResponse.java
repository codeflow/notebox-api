package com.notebox.api.api.dto;

import java.util.UUID;

/** Cleartext of one Secret value, returned only by the reveal endpoint — never listed or logged (FR-18). */
public record RevealResponse(UUID recordId, UUID fieldId, String value) {
}
