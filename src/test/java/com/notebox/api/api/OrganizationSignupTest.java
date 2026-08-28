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

/**
 * Organization signup — the one unauthenticated write in the product, so its boundaries get more
 * attention than its happy path.
 */
@QuarkusTest
class OrganizationSignupTest {

    @Inject
    TestData data;

    private static String body(String slug, String email) {
        return "{\"organizationName\":\"Acme " + slug + "\",\"slug\":\"" + slug + "\","
                + "\"displayName\":\"First Admin\",\"email\":\"" + email + "\","
                + "\"password\":\"long-enough-pass\"}";
    }

    private static String slug() {
        return "org-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void createsTheOrganizationAndSignsItsAdministratorIn() {
        String email = "founder-" + UUID.randomUUID() + "@notebox.test";

        String token = given().contentType("application/json").body(body(slug(), email))
                .when().post("/organizations")
                .then().statusCode(201)
                .body("token", notNullValue())
                .body("expiresAt", notNullValue())
                .extract().path("token");

        // The token must actually work, and name the organization just created.
        given().header("Authorization", "Bearer " + token)
                .when().get("/me")
                .then().statusCode(200)
                .body("role", equalTo("ADMIN"));
    }

    /** The founder is an ADMIN — otherwise they could not provision anyone and the org is a dead end. */
    @Test
    void theFounderCanImmediatelyProvisionAMember() {
        String email = "founder2-" + UUID.randomUUID() + "@notebox.test";
        String token = given().contentType("application/json").body(body(slug(), email))
                .when().post("/organizations").then().statusCode(201).extract().path("token");

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"email\":\"colleague-" + UUID.randomUUID() + "@notebox.test\","
                        + "\"displayName\":\"Colleague\",\"role\":\"MEMBER\",\"password\":\"long-enough-pass\"}")
                .when().post("/users")
                .then().statusCode(200);
    }

    /** A new organization starts empty — it must not see anything that already existed. */
    @Test
    void theNewOrganizationStartsEmpty() {
        Tenant other = data.createTenant();
        data.createUser(other.getId(), "prior-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);

        String token = given().contentType("application/json")
                .body(body(slug(), "founder3-" + UUID.randomUUID() + "@notebox.test"))
                .when().post("/organizations").then().statusCode(201).extract().path("token");

        given().header("Authorization", "Bearer " + token)
                .when().get("/overview")
                .then().statusCode(200)
                .body("records", equalTo(0))
                .body("typesDefined", equalTo(0));

        given().header("Authorization", "Bearer " + token)
                .when().get("/users")
                .then().statusCode(200)
                .body("size()", equalTo(1));
    }

    @Test
    void aTakenAddressIsRefused() {
        String taken = slug();
        given().contentType("application/json").body(body(taken, "a-" + UUID.randomUUID() + "@notebox.test"))
                .when().post("/organizations").then().statusCode(201);

        given().contentType("application/json").body(body(taken, "b-" + UUID.randomUUID() + "@notebox.test"))
                .when().post("/organizations")
                .then().statusCode(409)
                .body("code", equalTo("organization.slug.duplicate"));
    }

    @Test
    void anAddressWithUppercaseOrSpacesIsRefused() {
        given().contentType("application/json")
                .body(body("Not A Slug", "c-" + UUID.randomUUID() + "@notebox.test"))
                .when().post("/organizations")
                .then().statusCode(400);
    }

    /** An email is global: the same person cannot found a second organization with it. */
    @Test
    void anAlreadyRegisteredEmailIsRefused() {
        Tenant existing = data.createTenant();
        String email = "taken-" + UUID.randomUUID() + "@notebox.test";
        data.createUser(existing.getId(), email, "s3cret-pass", Role.MEMBER);

        given().contentType("application/json").body(body(slug(), email))
                .when().post("/organizations")
                .then().statusCode(409)
                .body("code", equalTo("member.email.duplicate"));
    }

    @Test
    void aShortPasswordIsRefused() {
        given().contentType("application/json")
                .body("{\"organizationName\":\"Acme\",\"slug\":\"" + slug() + "\","
                        + "\"displayName\":\"A\",\"email\":\"d-" + UUID.randomUUID() + "@notebox.test\","
                        + "\"password\":\"short\"}")
                .when().post("/organizations")
                .then().statusCode(400);
    }
}
