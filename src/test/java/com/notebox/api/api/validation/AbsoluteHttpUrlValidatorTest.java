package com.notebox.api.api.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A card link must be an absolute http/https URL with an authority (FR-13; audit feat-012 finding
 * 1): scheme-only, opaque and authority-less forms are unstorable, as are non-http schemes,
 * relative paths and unparseable values. Null passes — absence is the caller's concern.
 */
class AbsoluteHttpUrlValidatorTest {

    private final AbsoluteHttpUrlValidator validator = new AbsoluteHttpUrlValidator();

    @Test
    void nullPasses() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void acceptsAbsoluteHttpAndHttpsWithAnAuthority() {
        for (String ok : new String[] {
                "https://tracker.example/PAY-231",
                "http://localhost:8080/a?b=c#d",
                "HTTPS://X.EXAMPLE/Upper",
                "https://[::1]/",
                "https://user@host.example/path",
                "https://exämple.com/idn"}) {
            assertTrue(validator.isValid(ok, null), ok);
        }
    }

    @Test
    void rejectsAuthorityLessAndOpaqueForms() {
        for (String bad : new String[] {
                "https:foo", "https:/foo", "https:///path", "https:?q", "http:foo@bar",
                "https:javascript:alert(1)", "https://", "http://", "http:"}) {
            assertFalse(validator.isValid(bad, null), bad);
        }
    }

    @Test
    void rejectsOtherSchemesRelativeAndUnparseable() {
        for (String bad : new String[] {
                "javascript:alert(1)", "data:text/html;base64,x", "ftp://files.example/x",
                "/PAY-231", "PAY-231", "", " https://x.example", "https://ex ample.com/a",
                "https://example.com/a b", "https://example.com/%zz"}) {
            assertFalse(validator.isValid(bad, null), bad);
        }
    }
}
