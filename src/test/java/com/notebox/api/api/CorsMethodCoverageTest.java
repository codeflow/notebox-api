package com.notebox.api.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Every HTTP method the API exposes must be allowed by CORS.
 *
 * <p><b>Written because it was not true.</b> {@code PATCH /users/{id}} shipped in feat-001 and was
 * unreachable from a browser until 2026-09-01: the CORS method list simply did not contain PATCH.
 * Nothing caught it for months. The API's own tests call the endpoint directly through RestAssured,
 * where CORS does not exist; the web project's tests intercept at the fetch layer, where it does not
 * apply either. It surfaced only when a real browser tried to save a member's language, and the
 * request never left the page.
 *
 * <p>This reads the two sources against each other — the JAX-RS annotations on the resource classes
 * and the configured allow-list — so the next method added to a resource cannot be silently
 * unreachable. It is a plain unit test on purpose: it needs no container, so it runs in seconds and
 * cannot be skipped for being slow.
 */
class CorsMethodCoverageTest {

    private static final Path RESOURCES = Path.of("src/main/java/com/notebox/api/api");
    private static final Path CONFIG = Path.of("src/main/resources/application.properties");

    /** `@GET`, `@POST`, … as written on a resource method. */
    private static final Pattern JAX_RS_METHOD = Pattern.compile("@(GET|POST|PUT|PATCH|DELETE|HEAD)\\b");

    private static final Pattern CORS_METHODS =
            Pattern.compile("^%(dev|prod)\\.quarkus\\.http\\.cors\\.methods=(.+)$", Pattern.MULTILINE);

    @Test
    void everyMethodTheApiExposesIsAllowedByCors() {
        Set<String> exposed = exposedMethods();
        // Sanity: without this, an empty read would make every assertion below pass vacuously.
        assertTrue(
                exposed.containsAll(Set.of("GET", "POST", "PUT", "PATCH", "DELETE")),
                "the resource classes were actually read; found " + exposed);

        Matcher matcher = CORS_METHODS.matcher(read(CONFIG));
        int profiles = 0;
        while (matcher.find()) {
            profiles += 1;
            String profile = matcher.group(1);
            Set<String> allowed = Set.of(matcher.group(2).trim().split("\\s*,\\s*"));

            assertTrue(
                    allowed.containsAll(exposed),
                    "CORS for the '" + profile + "' profile allows " + allowed
                            + " but the API exposes " + exposed
                            + " — a method missing here is unreachable from any browser");
        }

        assertEquals(2, profiles, "both the dev and prod profiles declare a CORS method list");
    }

    /** The HTTP methods actually annotated on the resource classes. */
    private static Set<String> exposedMethods() {
        Set<String> methods = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                Matcher matcher = JAX_RS_METHOD.matcher(read(path));
                while (matcher.find()) {
                    methods.add(matcher.group(1));
                }
            });
        } catch (IOException cause) {
            throw new UncheckedIOException(cause);
        }
        return methods;
    }

    private static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException cause) {
            throw new UncheckedIOException(cause);
        }
    }
}
