package com.notebox.api.infrastructure.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Exercises the security layer + TenantContext population through the test-only /test/whoami endpoint.
 * Status codes are asserted here; the localized error body/codes are covered by T-06/T-07.
 */
@QuarkusTest
class JwtTenantFilterTest {

    @Test
    void missingTokenIsRejected() {
        given().when().get("/test/whoami").then().statusCode(401);
    }

    @Test
    void expiredTokenIsRejected() {
        String token = TestTokens.expired(UUID.randomUUID(), UUID.randomUUID(), "MEMBER");
        given().header("Authorization", "Bearer " + token)
                .when().get("/test/whoami")
                .then().statusCode(401);
    }

    @Test
    void invalidSignatureIsRejected() {
        String token = TestTokens.badSignature(UUID.randomUUID(), UUID.randomUUID(), "MEMBER");
        given().header("Authorization", "Bearer " + token)
                .when().get("/test/whoami")
                .then().statusCode(401);
    }

    @Test
    void tenantContextComesFromTokenNotFromInput() {
        UUID userId = UUID.randomUUID();
        UUID tokenTenant = UUID.randomUUID();
        UUID spoofedTenant = UUID.randomUUID();
        String token = TestTokens.valid(userId, tokenTenant, "MEMBER");

        given().header("Authorization", "Bearer " + token)
                .queryParam("tenantOverride", spoofedTenant.toString())
                .when().get("/test/whoami")
                .then().statusCode(200)
                .body("tenantId", equalTo(tokenTenant.toString()))
                .body("userId", equalTo(userId.toString()))
                .body("role", equalTo("MEMBER"));
    }
}
