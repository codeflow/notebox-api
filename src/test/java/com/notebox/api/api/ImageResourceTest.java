package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Binary icon upload/download over HTTP: tenant-scoped, authenticated (FR-07, AD-04, C-01, C-02). */
@QuarkusTest
class ImageResourceTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47};

    @Inject
    TestData data;

    private String token(Tenant tenant) {
        return TestTokens.valid(UUID.randomUUID(), tenant.getId(), Role.MEMBER.name());
    }

    @Test
    void uploadAndRetrieveIcon() {
        Tenant tenant = data.createTenant();
        String token = token(tenant);

        String id = given().header("Authorization", "Bearer " + token)
                .contentType("image/png").body(PNG)
                .when().post("/images")
                .then().statusCode(201)
                .body("contentType", equalTo("image/png"))
                .body("sizeBytes", equalTo(4))
                .extract().path("id");

        given().header("Authorization", "Bearer " + token)
                .when().get("/images/" + id)
                .then().statusCode(200)
                .contentType("image/png");
    }

    @Test
    void foreignTenantCannotReadImage() {
        Tenant tenantA = data.createTenant();
        Tenant tenantB = data.createTenant();

        String id = given().header("Authorization", "Bearer " + token(tenantA))
                .contentType("image/png").body(PNG)
                .when().post("/images")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + token(tenantB))
                .when().get("/images/" + id)
                .then().statusCode(404)
                .body("code", equalTo("annotation.image.not_found"));
    }

    @Test
    void unsupportedContentTypeIsRejected() {
        Tenant tenant = data.createTenant();

        given().header("Authorization", "Bearer " + token(tenant))
                .contentType("image/svg+xml").body(new byte[] {1, 2})
                .when().post("/images")
                .then().statusCode(400)
                .body("code", equalTo("annotation.image.type.unsupported"));
    }

    @Test
    void unauthenticatedIsRejected() {
        given().when().get("/images/" + UUID.randomUUID())
                .then().statusCode(401);
    }
}
