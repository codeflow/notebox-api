package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** End-to-end identity + isolation: real login token driving /me and /users/{id} across tenants (BR-01). */
@QuarkusTest
class UserResourceTest {

    @Inject
    TestData data;

    private String login(String email, String password) {
        return given().contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }

    @Test
    void meReturnsOwnIdentity() {
        Tenant tenant = data.createTenant();
        String email = "me-" + UUID.randomUUID() + "@t.test";
        User user = data.createUser(tenant.getId(), email, "pw-secret", Role.MEMBER);
        String token = login(email, "pw-secret");

        given().header("Authorization", "Bearer " + token)
                .when().get("/me")
                .then().statusCode(200)
                .body("userId", equalTo(user.getId().toString()))
                .body("tenantId", equalTo(tenant.getId().toString()));
    }

    @Test
    void readsOwnTenantUser() {
        Tenant tenant = data.createTenant();
        String email = "self-" + UUID.randomUUID() + "@t.test";
        User user = data.createUser(tenant.getId(), email, "pw-secret", Role.MEMBER);
        String token = login(email, "pw-secret");

        given().header("Authorization", "Bearer " + token)
                .when().get("/users/" + user.getId())
                .then().statusCode(200)
                .body("id", equalTo(user.getId().toString()))
                .body("passwordHash", nullValue());
    }

    @Test
    void crossTenantReadReturnsNotFound() {
        Tenant tenantA = data.createTenant();
        String emailA = "a-" + UUID.randomUUID() + "@t.test";
        data.createUser(tenantA.getId(), emailA, "pw-secret", Role.MEMBER);
        Tenant tenantB = data.createTenant();
        User userB = data.createUser(tenantB.getId(), "b-" + UUID.randomUUID() + "@t.test", "pw-secret", Role.MEMBER);

        String tokenA = login(emailA, "pw-secret");
        given().header("Authorization", "Bearer " + tokenA)
                .when().get("/users/" + userB.getId())
                .then().statusCode(404)
                .body("code", equalTo("RESOURCE_NOT_FOUND"));
    }

    @Test
    void crossTenantWriteIsDeniedAndLeavesDataUnchanged() {
        Tenant tenantA = data.createTenant();
        String emailA = "wa-" + UUID.randomUUID() + "@t.test";
        data.createUser(tenantA.getId(), emailA, "pw-secret", Role.MEMBER);
        Tenant tenantB = data.createTenant();
        String emailB = "wb-" + UUID.randomUUID() + "@t.test";
        User userB = data.createUser(tenantB.getId(), emailB, "pw-secret", Role.MEMBER);

        String tokenA = login(emailA, "pw-secret");
        given().header("Authorization", "Bearer " + tokenA)
                .contentType("application/json")
                .body("{\"displayName\":\"Hacked\"}")
                .when().patch("/users/" + userB.getId())
                .then().statusCode(404);

        String tokenB = login(emailB, "pw-secret");
        given().header("Authorization", "Bearer " + tokenB)
                .when().get("/users/" + userB.getId())
                .then().statusCode(200)
                .body("displayName", equalTo("Name"));
    }

    @Test
    void patchesOwnUser() {
        Tenant tenant = data.createTenant();
        String email = "pu-" + UUID.randomUUID() + "@t.test";
        User user = data.createUser(tenant.getId(), email, "pw-secret", Role.MEMBER);
        String token = login(email, "pw-secret");

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"displayName\":\"Ada Updated\"}")
                .when().patch("/users/" + user.getId())
                .then().statusCode(200)
                .body("displayName", equalTo("Ada Updated"));
    }
}
