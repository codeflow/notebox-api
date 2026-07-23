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
}
