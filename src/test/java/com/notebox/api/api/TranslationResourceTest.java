package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Runtime translation management (FR-16). */
@QuarkusTest
class TranslationResourceTest {

    private static final String KEY = "group.not_found";

    @Inject
    TestData data;

    private User adminOf(Tenant tenant) {
        return data.createUser(
                tenant.getId(), "tr-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.ADMIN);
    }

    private String admin(Tenant tenant, User user) {
        return "Bearer " + TestTokens.valid(user.getId(), tenant.getId(), "ADMIN");
    }

    @Test
    void theCatalogListsTheProductsOwnWording() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);

        given().header("Authorization", admin(tenant, boss))
                .when().get("/translations?locale=en")
                .then().statusCode(200)
                .body("key", hasItem(KEY))
                .body("find { it.key == '" + KEY + "' }.overridden", equalTo(false));
    }

    /** The point of the whole feature: a reworded message reaches the client without a deploy. */
    @Test
    void anOverrideReachesTheActualResponse() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);

        given().header("Authorization", admin(tenant, boss))
                .contentType("application/json")
                .body("{\"value\":\"Nothing by that name here.\"}")
                .when().put("/translations/" + KEY + "?locale=en")
                .then().statusCode(200)
                .body("overridden", equalTo(true));

        // Provoke the real error and read the message the API actually emits.
        given().header("Authorization", admin(tenant, boss))
                .header("Accept-Language", "en")
                .when().get("/groups/" + UUID.randomUUID())
                .then().statusCode(404)
                .body("message", equalTo("Nothing by that name here."));
    }

    @Test
    void clearingAnOverrideRestoresTheProductsWording() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);

        given().header("Authorization", admin(tenant, boss))
                .contentType("application/json").body("{\"value\":\"Temporary wording.\"}")
                .when().put("/translations/" + KEY + "?locale=en").then().statusCode(200);

        given().header("Authorization", admin(tenant, boss))
                .when().delete("/translations/" + KEY + "?locale=en")
                .then().statusCode(204);

        given().header("Authorization", admin(tenant, boss))
                .header("Accept-Language", "en")
                .when().get("/groups/" + UUID.randomUUID())
                .then().statusCode(404)
                .body("message", not(equalTo("Temporary wording.")));
    }

    /** One tenant's wording must never be read by another (BR-01, BR-02). */
    @Test
    void anOverrideNeverCrossesTenants() {
        Tenant mine = data.createTenant();
        Tenant theirs = data.createTenant();
        User myBoss = adminOf(mine);
        User theirBoss = adminOf(theirs);

        given().header("Authorization", admin(theirs, theirBoss))
                .contentType("application/json").body("{\"value\":\"Their private wording.\"}")
                .when().put("/translations/" + KEY + "?locale=en").then().statusCode(200);

        given().header("Authorization", admin(mine, myBoss))
                .header("Accept-Language", "en")
                .when().get("/groups/" + UUID.randomUUID())
                .then().statusCode(404)
                .body("message", not(equalTo("Their private wording.")));

        given().header("Authorization", admin(mine, myBoss))
                .when().get("/translations?locale=en")
                .then().statusCode(200)
                .body("find { it.key == '" + KEY + "' }.overridden", equalTo(false));
    }

    /** A tenant rewords what the product says; it does not invent messages. */
    @Test
    void anUnknownKeyIsRefusedRatherThanCreated() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);

        given().header("Authorization", admin(tenant, boss))
                .contentType("application/json").body("{\"value\":\"Anything\"}")
                .when().put("/translations/not.a.real.key?locale=en")
                .then().statusCode(404)
                .body("code", equalTo("translation.key.unknown"));
    }

    @Test
    void blankWordingIsRefused() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);

        given().header("Authorization", admin(tenant, boss))
                .contentType("application/json").body("{\"value\":\"  \"}")
                .when().put("/translations/" + KEY + "?locale=en")
                .then().statusCode(400);
    }

    /** C-03: wording is what every member reads, so changing it is administrative. */
    @Test
    void anOrdinaryMemberMayNotEditTheCatalog() {
        Tenant tenant = data.createTenant();
        User plain = data.createUser(
                tenant.getId(), "plain-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);

        given().header("Authorization", "Bearer " + TestTokens.valid(plain.getId(), tenant.getId(), "MEMBER"))
                .when().get("/translations?locale=en")
                .then().statusCode(403);
    }
}
