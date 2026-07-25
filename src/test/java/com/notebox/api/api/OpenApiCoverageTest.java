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

        assertTrue(document.contains("annotation-records"), "annotation-records endpoints missing from OpenAPI");
        assertTrue(document.contains("/annotation-records/{id}/values/{fieldId}/reveal"),
                "reveal endpoint missing from OpenAPI");
    }
}
