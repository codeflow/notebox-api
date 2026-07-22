package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthResourceTest {

    @Inject
    TestData data;

    @Test
    void validCredentialsYieldToken() {
        Tenant tenant = data.createTenant();
        String email = "valid-" + UUID.randomUUID() + "@notebox.test";
        data.createUser(tenant.getId(), email, "s3cret-pass", Role.MEMBER);

        given().contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"password\":\"s3cret-pass\"}")
                .when().post("/auth/login")
                .then().statusCode(200)
                .body("token", notNullValue())
                .body("expiresAt", notNullValue());
    }

    @Test
    void invalidPasswordIsRejected() {
        Tenant tenant = data.createTenant();
        String email = "wrongpw-" + UUID.randomUUID() + "@notebox.test";
        data.createUser(tenant.getId(), email, "s3cret-pass", Role.MEMBER);

        given().contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"password\":\"WRONG\"}")
                .when().post("/auth/login")
                .then().statusCode(401)
                .body("code", equalTo("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void unknownEmailIsRejected() {
        given().contentType("application/json")
                .body("{\"email\":\"nobody-" + UUID.randomUUID() + "@notebox.test\",\"password\":\"x\"}")
                .when().post("/auth/login")
                .then().statusCode(401)
                .body("code", equalTo("AUTH_INVALID_CREDENTIALS"));
    }
}
