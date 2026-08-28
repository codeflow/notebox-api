package com.notebox.api.api.dto;

/**
 * One row of the translation catalog (design 17, FR-16): the product's own wording, this tenant's
 * override if any, and which of the two is in force.
 */
public record TranslationDto(String key, String defaultValue, String overrideValue, boolean overridden) {
}
