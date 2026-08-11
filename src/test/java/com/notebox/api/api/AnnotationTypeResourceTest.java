package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** End-to-end annotation-type CRUD with tenant isolation and auth (FR-01, C-01, C-02). */
@QuarkusTest
class AnnotationTypeResourceTest {

    private static final String TYPE_JSON = """
            {"name":"RabbitMQ","fields":[
              {"name":"URL","fieldType":"TEXT"},
              {"name":"Status","fieldType":"LIST","options":[{"label":"open","badgeColour":"GREEN"}]}
            ]}""";

    @Inject
    TestData data;

    private String token(Tenant tenant) {
        return TestTokens.valid(UUID.randomUUID(), tenant.getId(), Role.MEMBER.name());
    }

    private String createType(String token) {
        return given().header("Authorization", "Bearer " + token)
                .contentType("application/json").body(TYPE_JSON)
                .when().post("/annotation-types")
                .then().statusCode(201)
                .body("name", equalTo("RabbitMQ"))
                .body("fields[0].name", equalTo("URL"))
                .body("fields[1].name", equalTo("Status"))
                .extract().path("id");
    }

    @Test
    void createThenReadPreservesOrderAndOptions() {
        String token = token(data.createTenant());
        String id = createType(token);

        given().header("Authorization", "Bearer " + token)
                .when().get("/annotation-types/" + id)
                .then().statusCode(200)
                .body("fields[0].name", equalTo("URL"))
                .body("fields[1].options[0].badgeColour", equalTo("GREEN"));
    }

    @Test
    void foreignTenantCannotReadOrDelete() {
        String id = createType(token(data.createTenant()));
        String otherToken = token(data.createTenant());

        given().header("Authorization", "Bearer " + otherToken)
                .when().get("/annotation-types/" + id)
                .then().statusCode(404)
                .body("code", equalTo("annotation.type.not_found"));

        given().header("Authorization", "Bearer " + otherToken)
                .when().delete("/annotation-types/" + id)
                .then().statusCode(404);
    }

    @Test
    void deleteRemovesTheType() {
        String token = token(data.createTenant());
        String id = createType(token);

        given().header("Authorization", "Bearer " + token)
                .when().delete("/annotation-types/" + id)
                .then().statusCode(204);

        given().header("Authorization", "Bearer " + token)
                .when().get("/annotation-types/" + id)
                .then().statusCode(404);
    }

    @Test
    void blankNameIsRejectedWithLocalizedViolation() {
        String token = token(data.createTenant());

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json").body("{\"name\":\"\",\"fields\":[]}")
                .when().post("/annotation-types")
                .then().statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("violations.code", hasItem("annotation.type.name.required"));
    }

    @Test
    void unauthenticatedIsRejected() {
        given().when().get("/annotation-types/" + UUID.randomUUID())
                .then().statusCode(401);
    }

    private static final String GUARDED_TYPE_JSON = """
            {"name":"Broker","fields":[
              {"name":"URL","fieldType":"TEXT"},
              {"name":"API key","fieldType":"TEXT","secret":true}
            ]}""";

    /** Type id, record id and URL-field id of a guarded type whose record holds both values. */
    private record PopulatedType(String typeId, String recordId, String urlFieldId) {}

    private PopulatedType createPopulatedType(String token) {
        var type = given().header("Authorization", "Bearer " + token)
                .contentType("application/json").body(GUARDED_TYPE_JSON)
                .when().post("/annotation-types")
                .then().statusCode(201)
                .extract().response();
        String typeId = type.jsonPath().getString("id");
        String urlFieldId = type.jsonPath().getString("fields[0].id");
        String recordId = given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"annotationTypeId":"%s","name":"prod","values":[
                          {"fieldId":"%s","text":"amqp://h"},
                          {"fieldId":"%s","text":"s3cr3t"}
                        ]}""".formatted(typeId, urlFieldId, type.jsonPath().getString("fields[1].id")))
                .when().post("/annotation-records")
                .then().statusCode(201)
                .extract().path("id");
        return new PopulatedType(typeId, recordId, urlFieldId);
    }

    /** The guarded state must be observably unchanged after a 409 (spec post-state clauses, audit R2-06). */
    private void assertGuardedStateUnchanged(String token, PopulatedType populated) {
        given().header("Authorization", "Bearer " + token)
                .when().get("/annotation-types/" + populated.typeId())
                .then().statusCode(200)
                .body("fields.name", hasItem("URL"))
                .body("fields.name", hasItem("API key"))
                .body("fields.size()", equalTo(2));
        given().header("Authorization", "Bearer " + token)
                .when().get("/annotation-records/" + populated.recordId())
                .then().statusCode(200)
                .body("values.size()", equalTo(2))
                .body("values.find { it.fieldId == '" + populated.urlFieldId() + "' }.text",
                        equalTo("amqp://h"));
    }

    @Test
    void deletingAPopulatedTypeIsA409AndChangesNothing() {
        String token = token(data.createTenant());
        PopulatedType populated = createPopulatedType(token);

        given().header("Authorization", "Bearer " + token)
                .when().delete("/annotation-types/" + populated.typeId())
                .then().statusCode(409)
                .body("code", equalTo("annotation.type.has_records"));

        assertGuardedStateUnchanged(token, populated);
    }

    @Test
    void removingAFieldOfAPopulatedTypeIsA409AndChangesNothing() {
        String token = token(data.createTenant());
        PopulatedType populated = createPopulatedType(token);

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"name\":\"Broker\",\"fields\":[{\"name\":\"URL\",\"fieldType\":\"TEXT\"}]}")
                .when().put("/annotation-types/" + populated.typeId())
                .then().statusCode(409)
                .body("code", equalTo("annotation.type.field.has_records"));

        assertGuardedStateUnchanged(token, populated);
    }

    @Test
    void flippingTheSecretFlagOfAFieldHoldingValuesIsA409AndChangesNothing() {
        String token = token(data.createTenant());
        PopulatedType populated = createPopulatedType(token);

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"name":"Broker","fields":[
                          {"name":"URL","fieldType":"TEXT","secret":true},
                          {"name":"API key","fieldType":"TEXT","secret":true}
                        ]}""")
                .when().put("/annotation-types/" + populated.typeId())
                .then().statusCode(409)
                .body("code", equalTo("annotation.type.field.secret_flip.has_values"));

        // URL stayed non-secret and readable; the secret stayed masked (spec scenarios 26/27 post-states)
        assertGuardedStateUnchanged(token, populated);
        given().header("Authorization", "Bearer " + token)
                .when().get("/annotation-records/" + populated.recordId())
                .then().statusCode(200)
                .body("values.find { it.fieldId == '" + populated.urlFieldId() + "' }.masked",
                        equalTo(false));
    }
}
