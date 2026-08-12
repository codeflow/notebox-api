package com.notebox.api.infrastructure.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The shared dialect corpus (feat-007 T-01) — mirrors the web client's sanitize.test.ts
 * case-for-case, so the two enforcement points of the one dialect cannot drift apart silently.
 * Plain JUnit: the sanitizer needs no container.
 */
class JsoupRichTextSanitizerTest {

    private final JsoupRichTextSanitizer sanitizer = new JsoupRichTextSanitizer();

    @Test
    void dropsScriptElementsAndTheirContentEntirely() {
        String out = sanitizer.sanitize("<p>before</p><script>alert(\"x\")</script><p>after</p>");
        assertTrue(out.contains("before"));
        assertTrue(out.contains("after"));
        assertFalse(out.contains("script"));
        assertFalse(out.contains("alert"));
    }

    @Test
    void stripsEventHandlerAttributes() {
        String out = sanitizer.sanitize(
                "<p onclick=\"steal()\">text</p><img data-image-id=\"i1\" onerror=\"steal()\">");
        assertFalse(out.contains("onclick"));
        assertFalse(out.contains("onerror"));
        assertFalse(out.contains("steal"));
        assertTrue(out.contains("text"));
        assertTrue(out.contains("data-image-id=\"i1\""));
    }

    @Test
    void dropsIframes() {
        String out = sanitizer.sanitize("<p>ok</p><iframe src=\"https://evil.example\"></iframe>");
        assertFalse(out.contains("iframe"));
        assertFalse(out.contains("evil.example"));
        assertTrue(out.contains("ok"));
    }

    @Test
    void removesJavascriptAndDataHrefsButKeepsTheLinkText() {
        String js = sanitizer.sanitize("<a href=\"javascript:alert(1)\">click</a>");
        assertFalse(js.contains("javascript:"));
        assertTrue(js.contains("click"));

        String data = sanitizer.sanitize("<a href=\"data:text/html,x\">click</a>");
        assertFalse(data.contains("data:"));
        assertTrue(data.contains("click"));
    }

    @Test
    void keepsHttpLinksAndForcesRel() {
        String out = sanitizer.sanitize(
                "<a href=\"https://example.com\" target=\"_blank\" rel=\"opener\">docs</a>");
        assertTrue(out.contains("href=\"https://example.com\""));
        assertTrue(out.contains("rel=\"noopener noreferrer\""));
        assertFalse(out.contains("target"));
    }

    @Test
    void stripsImgSrcAlwaysKeepingTheDialectReference() {
        String out = sanitizer.sanitize(
                "<img src=\"https://evil.example/pixel.png\" data-image-id=\"0d4f2c1a\" alt=\"diagram\">");
        assertFalse(out.contains("src="));
        assertFalse(out.contains("evil.example"));
        assertTrue(out.contains("data-image-id=\"0d4f2c1a\""));
        assertTrue(out.contains("alt=\"diagram\""));
    }

    @Test
    void dropsArbitraryDataAttributes() {
        String out = sanitizer.sanitize(
                "<p data-evil=\"x\">text</p><img data-image-id=\"i1\" data-track=\"y\">");
        assertFalse(out.contains("data-evil"));
        assertFalse(out.contains("data-track"));
        assertTrue(out.contains("data-image-id=\"i1\""));
    }

    @Test
    void dropsUnknownElementsKeepingTheirText() {
        String out = sanitizer.sanitize("<marquee>hi</marquee><blink>there</blink>");
        assertFalse(out.contains("marquee"));
        assertFalse(out.contains("blink"));
        assertTrue(out.contains("hi"));
        assertTrue(out.contains("there"));
    }

    @Test
    void dialectAttributesOnTheWrongElementAreDropped() {
        String out = sanitizer.sanitize("<p data-language=\"sql\" href=\"https://x.example\">text</p>");
        assertFalse(out.contains("data-language"));
        assertFalse(out.contains("href"));
        assertTrue(out.contains("text"));
    }

    @Test
    void preservesTheCanonicalDialectDocumentByteIdentically() {
        String dialect =
                "<p><strong>b</strong> <em>i</em> <u>u</u> <s>s</s> <code>c</code></p>"
                        + "<ol><li><p>one</p></li></ol><ul><li><p>two</p></li></ul>"
                        + "<blockquote><p>q</p></blockquote>"
                        + "<pre data-language=\"sql\"><code>SELECT 1</code></pre>"
                        + "<p><span style=\"color: rgb(200, 30, 30);\">warn</span></p>"
                        + "<p><a href=\"https://example.com\" rel=\"noopener noreferrer\">docs</a></p>"
                        + "<img data-image-id=\"abc123\" alt=\"d\">";
        assertEquals(dialect, sanitizer.sanitize(dialect));
    }

    @Test
    void keepsOnlyTheColourDeclarationOfASpanStyle() {
        String out = sanitizer.sanitize(
                "<p><span style=\"color: rgb(200, 30, 30); position: fixed; font-size: 80px\">warn</span></p>");
        assertTrue(out.contains("color:"));
        assertFalse(out.contains("position"));
        assertFalse(out.contains("font-size"));
        assertTrue(out.contains("warn"));
    }

    @Test
    void isIdempotentOverMessyInput() {
        String messy = "<p><span style=\"color: red; top: 0\">x</span></p>"
                + "<a href=\"https://a.example\">a</a>"
                + "<img data-image-id=\"i1\" src=\"https://evil.example/p.png\">"
                + "<p>a &amp; b &lt;tag&gt;</p>";
        String once = sanitizer.sanitize(messy);
        assertEquals(once, sanitizer.sanitize(once));
    }

    @Test
    void nullAndEmptyPassThrough() {
        assertEquals(null, sanitizer.sanitize(null));
        assertEquals("", sanitizer.sanitize(""));
    }
}
