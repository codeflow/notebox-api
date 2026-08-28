package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Member administration (FR-17, OQ-11). This is the first surface in the app to carry a role check,
 * so C-03 gets as much attention here as C-01.
 */
@QuarkusTest
class MemberAdministrationTest {

    @Inject
    TestData data;

    private String admin(Tenant tenant, User user) {
        return "Bearer " + TestTokens.valid(user.getId(), tenant.getId(), "ADMIN");
    }

    private String member(Tenant tenant, User user) {
        return "Bearer " + TestTokens.valid(user.getId(), tenant.getId(), "MEMBER");
    }

    private User adminOf(Tenant tenant) {
        return data.createUser(
                tenant.getId(), "admin-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.ADMIN);
    }

    @Test
    void anAdministratorProvisionsAMember() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);
        String email = "new-" + UUID.randomUUID() + "@notebox.test";

        given().header("Authorization", admin(tenant, boss))
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"displayName\":\"New Person\","
                        + "\"role\":\"MEMBER\",\"password\":\"long-enough-pass\"}")
                .when().post("/users")
                .then().statusCode(200)
                .body("email", equalTo(email))
                .body("role", equalTo("MEMBER"))
                .body("tenantId", equalTo(tenant.getId().toString()))
                .body("active", equalTo(true));
    }

    /** The provisioned member must actually be able to sign in — otherwise nothing was provisioned. */
    @Test
    void theProvisionedMemberCanSignIn() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);
        String email = "signin-" + UUID.randomUUID() + "@notebox.test";

        given().header("Authorization", admin(tenant, boss))
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"displayName\":\"Signs In\","
                        + "\"role\":\"MEMBER\",\"password\":\"long-enough-pass\"}")
                .when().post("/users").then().statusCode(200);

        given().contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"password\":\"long-enough-pass\"}")
                .when().post("/auth/login")
                .then().statusCode(200);
    }

    /** C-03: provisioning is not ordinary member data. A MEMBER token must be refused. */
    @Test
    void anOrdinaryMemberMayNotProvision() {
        Tenant tenant = data.createTenant();
        User plain = data.createUser(
                tenant.getId(), "plain-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);

        given().header("Authorization", member(tenant, plain))
                .contentType("application/json")
                .body("{\"email\":\"x-" + UUID.randomUUID() + "@notebox.test\",\"displayName\":\"X\","
                        + "\"role\":\"MEMBER\",\"password\":\"long-enough-pass\"}")
                .when().post("/users")
                .then().statusCode(403);
    }

    @Test
    void anOrdinaryMemberMayNotListMembers() {
        Tenant tenant = data.createTenant();
        User plain = data.createUser(
                tenant.getId(), "plain2-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);

        given().header("Authorization", member(tenant, plain))
                .when().get("/users")
                .then().statusCode(403);
    }

    /** C-01: the listing is the caller's tenant, never anyone else's. */
    @Test
    void theListingNeverCrossesTenants() {
        Tenant mine = data.createTenant();
        Tenant theirs = data.createTenant();
        User boss = adminOf(mine);
        User stranger = data.createUser(
                theirs.getId(), "stranger-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);

        given().header("Authorization", admin(mine, boss))
                .when().get("/users")
                .then().statusCode(200)
                .body("email", hasItem(boss.getEmail()))
                .body("email", not(hasItem(stranger.getEmail())));
    }

    /** The administered answer to "Forgot password?": the new password works, the old one stops. */
    @Test
    void anAdministratorResetsAMemberPassword() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);
        String email = "reset-" + UUID.randomUUID() + "@notebox.test";
        User target = data.createUser(tenant.getId(), email, "old-password-x", Role.MEMBER);

        given().header("Authorization", admin(tenant, boss))
                .contentType("application/json")
                .body("{\"password\":\"brand-new-password\"}")
                .when().put("/users/" + target.getId() + "/password")
                .then().statusCode(204);

        given().contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"password\":\"brand-new-password\"}")
                .when().post("/auth/login").then().statusCode(200);

        given().contentType("application/json")
                .body("{\"email\":\"" + email + "\",\"password\":\"old-password-x\"}")
                .when().post("/auth/login").then().statusCode(401);
    }

    /** C-01: an administrator cannot reach into another tenant, even with a real member id. */
    @Test
    void anAdministratorCannotResetAForeignMemberPassword() {
        Tenant mine = data.createTenant();
        Tenant theirs = data.createTenant();
        User boss = adminOf(mine);
        User stranger = data.createUser(
                theirs.getId(), "foreign-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);

        given().header("Authorization", admin(mine, boss))
                .contentType("application/json")
                .body("{\"password\":\"brand-new-password\"}")
                .when().put("/users/" + stranger.getId() + "/password")
                .then().statusCode(404);
    }

    @Test
    void aDuplicateEmailIsRejectedWithoutNamingItsTenant() {
        Tenant mine = data.createTenant();
        Tenant theirs = data.createTenant();
        User boss = adminOf(mine);
        String taken = "taken-" + UUID.randomUUID() + "@notebox.test";
        data.createUser(theirs.getId(), taken, "s3cret-pass", Role.MEMBER);

        given().header("Authorization", admin(mine, boss))
                .contentType("application/json")
                .body("{\"email\":\"" + taken + "\",\"displayName\":\"Dup\","
                        + "\"role\":\"MEMBER\",\"password\":\"long-enough-pass\"}")
                .when().post("/users")
                .then().statusCode(409)
                .body("code", equalTo("member.email.duplicate"))
                // The other tenant is not named, hinted at, or counted.
                .body("message", not(org.hamcrest.Matchers.containsString(theirs.getSlug())));
    }

    @Test
    void aShortPasswordIsRejected() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);

        given().header("Authorization", admin(tenant, boss))
                .contentType("application/json")
                .body("{\"email\":\"short-" + UUID.randomUUID() + "@notebox.test\",\"displayName\":\"S\","
                        + "\"role\":\"MEMBER\",\"password\":\"short\"}")
                .when().post("/users")
                .then().statusCode(400);
    }

    @Test
    void anAdministratorCannotDeactivateThemselves() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);

        given().header("Authorization", admin(tenant, boss))
                .contentType("application/json")
                .body("{\"active\":false}")
                .when().put("/users/" + boss.getId() + "/active")
                .then().statusCode(400)
                .body("code", equalTo("member.deactivate.self"));
    }

    /** The hash must never leave the server, however the member is fetched. */
    @Test
    void noResponseEverCarriesAPasswordHash() {
        Tenant tenant = data.createTenant();
        User boss = adminOf(tenant);

        given().header("Authorization", admin(tenant, boss))
                .when().get("/users")
                .then().statusCode(200)
                .body("[0].passwordHash", nullValue());
    }
}
