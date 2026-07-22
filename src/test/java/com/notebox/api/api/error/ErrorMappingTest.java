package com.notebox.api.api.error;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Verifies the uniform localized error envelope across the auth failure modes (AD-11, BR-08, C-09). */
@QuarkusTest
class ErrorMappingTest {

    @Test
    void missingTokenYieldsAuthRequired() {
        given().when().get("/test/whoami")
                .then().statusCode(401)
                .body("code", equalTo("AUTH_REQUIRED"));
    }

    @Test
    void errorMessageHonoursRequestLocale() {
        given().header("Accept-Language", "pt")
                .when().get("/test/whoami")
                .then().statusCode(401)
                .body("code", equalTo("AUTH_REQUIRED"))
                .body("message", equalTo("Autenticação é obrigatória."));
    }

    @Test
    void expiredTokenYieldsExpiredCode() {
        String token = TestTokens.expired(UUID.randomUUID(), UUID.randomUUID(), "MEMBER");
        given().header("Authorization", "Bearer " + token)
                .when().get("/test/whoami")
                .then().statusCode(401)
                .body("code", equalTo("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    void invalidSignatureYieldsInvalidCode() {
        String token = TestTokens.badSignature(UUID.randomUUID(), UUID.randomUUID(), "MEMBER");
        given().header("Authorization", "Bearer " + token)
                .when().get("/test/whoami")
                .then().statusCode(401)
                .body("code", equalTo("AUTH_TOKEN_INVALID"));
    }
}
