package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Every new endpoint is present in the generated OpenAPI document (NFR-06). */
@QuarkusTest
class OpenApiCoverageTest {

    @Test
    void openApiDocumentsAnnotationTypeAndImageEndpoints() {
        String document = given().accept("application/json")
                .when().get("/q/openapi")
                .then().statusCode(200)
                .extract().asString();

        assertTrue(document.contains("annotation-types"), "annotation-types endpoints missing from OpenAPI");
        assertTrue(document.contains("images"), "image endpoints missing from OpenAPI");
    }

    @Test
    void openApiDocumentsAnnotationRecordEndpoints() {
        String document = given().accept("application/json")
                .when().get("/q/openapi")
                .then().statusCode(200)
                .extract().asString();

        assertTrue(hasPathKey(document, "/api/annotation-records"),
                "record collection path missing from OpenAPI (must be its own path key, not a substring)");
        assertTrue(hasPathKey(document, "/api/annotation-records/{id}"),
                "record item path missing from OpenAPI");
        assertTrue(hasPathKey(document, "/api/annotation-records/{id}/values/{fieldId}/reveal"),
                "reveal endpoint missing from OpenAPI");
    }

    /** Matches a whole path KEY (JSON quoted or YAML colon-terminated), never a substring of a longer path. */
    private static boolean hasPathKey(String document, String path) {
        return document.contains("\"" + path + "\"") || document.contains(path + ":");
    }
}
