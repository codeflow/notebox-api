package com.notebox.api.application.content;

/**
 * Sanitizes rich Free-text annotation values to the stored dialect (C-08, OQ-19 — feat-007).
 * The dialect is the allow-list the web client enforces on render, byte-for-byte: p, br, strong,
 * em, u, s, ol, ul, li, blockquote, code (no attributes); span (colour-only style); a (http/https
 * href, rel forced to "noopener noreferrer"); pre (data-language); img (data-image-id and alt,
 * never src). Identity for dialect-clean input; idempotent for all input.
 */
public interface RichTextSanitizer {

    /** Returns the dialect-clean form of {@code html}; null and empty pass through unchanged. */
    String sanitize(String html);
}
