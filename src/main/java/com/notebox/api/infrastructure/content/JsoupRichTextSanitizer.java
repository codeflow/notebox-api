package com.notebox.api.infrastructure.content;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.application.content.RichTextSanitizer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

/**
 * jsoup-backed dialect sanitizer (feat-007 contracts/sanitizer.md). The Safelist carries the
 * allow-list; the post-pass applies the two rules a Safelist cannot express — colour-only span
 * styles (kept verbatim when already clean, for byte-identity with the client dialect) and the
 * output shape that matches the browser serialization the web editor emits.
 */
@ApplicationScoped
public class JsoupRichTextSanitizer implements RichTextSanitizer {

    /** A style attribute that is already exactly one colour declaration — kept byte-verbatim. */
    private static final Pattern COLOUR_ONLY = Pattern.compile("(?i)^\\s*color\\s*:\\s*[^;<>]+;?\\s*$");

    /** Extracts the colour declaration out of a mixed style attribute. */
    private static final Pattern COLOUR_DECL = Pattern.compile("(?i)(?:^|;)\\s*color\\s*:\\s*([^;]+)");

    private final Safelist safelist = new Safelist()
            .addTags("p", "br", "strong", "em", "u", "s", "span", "ol", "ul", "li",
                    "blockquote", "a", "code", "pre", "img")
            .addAttributes("span", "style")
            .addAttributes("a", "href")
            .addAttributes("pre", "data-language")
            .addAttributes("img", "data-image-id", "alt")
            .addProtocols("a", "href", "http", "https")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer");

    @Override
    public String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        Document dirty = Jsoup.parseBodyFragment(html, "");
        Document clean = new Cleaner(safelist).clean(dirty);
        clean.outputSettings()
                .prettyPrint(false)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8");
        for (Element span : clean.body().select("span[style]")) {
            filterStyleToColour(span);
        }
        return clean.body().html();
    }

    /** Keeps a clean colour declaration verbatim; reduces mixed styles to their colour; drops the rest. */
    private void filterStyleToColour(Element span) {
        String style = span.attr("style");
        if (COLOUR_ONLY.matcher(style).matches()) {
            return;
        }
        Matcher colour = COLOUR_DECL.matcher(style);
        if (colour.find()) {
            span.attr("style", "color: " + colour.group(1).trim() + ";");
        } else {
            span.removeAttr("style");
        }
    }
}
