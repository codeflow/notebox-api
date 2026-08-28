package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** The workspace summary (OQ-31). Its whole risk is counting across tenants. */
@QuarkusTest
class OverviewResourceTest {

    @Inject
    TestData data;

    private String tokenFor(Tenant tenant, User user) {
        return "Bearer " + TestTokens.valid(user.getId(), tenant.getId(), "MEMBER");
    }

    private User memberOf(Tenant tenant) {
        return data.createUser(
                tenant.getId(), "ov-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);
    }

    @Test
    void anEmptyWorkspaceCountsZeroEverywhere() {
        Tenant tenant = data.createTenant();
        User user = memberOf(tenant);

        given().header("Authorization", tokenFor(tenant, user))
                .when().get("/overview")
                .then().statusCode(200)
                .body("typesDefined", equalTo(0))
                .body("records", equalTo(0))
                .body("recordsWithImages", equalTo(0))
                .body("secretFields", equalTo(0))
                .body("openTasks", equalTo(0))
                .body("subtasksDone", equalTo(0))
                .body("subtasksTotal", equalTo(0))
                .body("tasksEndingThisWeek", equalTo(0));
    }

    /**
     * The reason this endpoint needs a test at all: a count that forgot its tenant filter would
     * still return a plausible number, and nobody would notice until two customers compared totals.
     */
    @Test
    void neverCountsAnotherTenantsWork() {
        Tenant mine = data.createTenant();
        Tenant theirs = data.createTenant();
        User user = memberOf(mine);
        User stranger = memberOf(theirs);

        String theirToken = "Bearer " + TestTokens.valid(stranger.getId(), theirs.getId(), "MEMBER");
        given().header("Authorization", theirToken)
                .contentType("application/json")
                .body("{\"name\":\"Theirs\",\"fields\":[]}")
                .when().post("/annotation-types").then().statusCode(201);

        given().header("Authorization", tokenFor(mine, user))
                .when().get("/overview")
                .then().statusCode(200)
                .body("typesDefined", equalTo(0));
    }

    @Test
    void countsTheCallersOwnTypes() {
        Tenant tenant = data.createTenant();
        User user = memberOf(tenant);

        given().header("Authorization", tokenFor(tenant, user))
                .contentType("application/json")
                .body("{\"name\":\"Mine\",\"fields\":[]}")
                .when().post("/annotation-types").then().statusCode(201);

        given().header("Authorization", tokenFor(tenant, user))
                .when().get("/overview")
                .then().statusCode(200)
                .body("typesDefined", equalTo(1));
    }

    @Test
    void requiresAuthentication() {
        given().when().get("/overview").then().statusCode(401);
    }
}
