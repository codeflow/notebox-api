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
        assertTrue(document.contains("typeId"),
                "the listing GET's typeId parameter is documented (feat-008, NFR-06)");
    }

    @Test
    void openApiDocumentsTaskEndpoints() {
        String document = given().accept("application/json")
                .when().get("/q/openapi")
                .then().statusCode(200)
                .extract().asString();

        assertTrue(hasPathKey(document, "/api/tasks"),
                "task collection path missing from OpenAPI (feat-010, NFR-06)");
        assertTrue(hasPathKey(document, "/api/tasks/{id}"),
                "task item path missing from OpenAPI");
        assertTrue(hasPathKey(document, "/api/tasks/{id}/subtasks"),
                "subtask collection path missing from OpenAPI");
        assertTrue(hasPathKey(document, "/api/tasks/{id}/subtasks/{subtaskId}"),
                "subtask item path missing from OpenAPI");
    }

    @Test
    void openApiDocumentsGroupEndpoints() {
        String document = given().accept("application/json")
                .when().get("/q/openapi")
                .then().statusCode(200)
                .extract().asString();

        assertTrue(hasPathKey(document, "/api/groups"),
                "group collection path missing from OpenAPI (feat-014, NFR-06)");
        assertTrue(hasPathKey(document, "/api/groups/{id}"),
                "group item path missing from OpenAPI");
        assertTrue(document.contains("domain"),
                "the listing GET's domain parameter is documented (feat-014, NFR-06)");
        assertTrue(document.contains("itemCount") && document.contains("typesUsed")
                        && document.contains("averageStatus"),
                "the feat-016 aggregate fields are published in the schema (OQ-27, NFR-06)");
    }

    @Test
    void openApiDocumentsNavigationEndpoint() {
        String document = given().accept("application/json")
                .when().get("/q/openapi")
                .then().statusCode(200)
                .extract().asString();

        assertTrue(hasPathKey(document, "/api/navigation"),
                "navigation path missing from OpenAPI (feat-014, NFR-06)");
    }

    /** Matches a whole path KEY (JSON quoted or YAML colon-terminated), never a substring of a longer path. */
    private static boolean hasPathKey(String document, String path) {
        return document.contains("\"" + path + "\"") || document.contains(path + ":");
    }
}
