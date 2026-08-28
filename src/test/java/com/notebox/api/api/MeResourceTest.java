package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
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

/**
 * {@code /me} had no test at all until OQ-30 added the tenant's name and slug to its response.
 * These pin both the new fields and the isolation rule they depend on.
 */
@QuarkusTest
class MeResourceTest {

    @Inject
    TestData data;

    @Test
    void namesTheCallersOwnTenant() {
        Tenant tenant = data.createTenant();
        User user = data.createUser(
                tenant.getId(), "me-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);

        given().header("Authorization", "Bearer " + TestTokens.valid(user.getId(), tenant.getId(), "MEMBER"))
                .when().get("/me")
                .then().statusCode(200)
                .body("tenantId", equalTo(tenant.getId().toString()))
                .body("tenantName", equalTo(tenant.getName()))
                .body("tenantSlug", equalTo(tenant.getSlug()))
                .body("displayName", equalTo(user.getDisplayName()))
                .body("role", equalTo("MEMBER"));
    }

    /**
     * The reason {@code TenantRepository} is not tenant-scoped: it reads the tenant row directly, so
     * the only thing keeping it honest is that the id comes from the token. A member of tenant A must
     * never see tenant B's name, however the request is shaped.
     */
    @Test
    void neverNamesAnotherTenant() {
        Tenant mine = data.createTenant();
        Tenant theirs = data.createTenant();
        User user = data.createUser(
                mine.getId(), "iso-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);

        given().header("Authorization", "Bearer " + TestTokens.valid(user.getId(), mine.getId(), "MEMBER"))
                .when().get("/me")
                .then().statusCode(200)
                .body("tenantSlug", equalTo(mine.getSlug()))
                .body("tenantSlug", not(equalTo(theirs.getSlug())))
                .body("tenantId", not(equalTo(theirs.getId().toString())));
    }

    @Test
    void requiresAuthentication() {
        given().when().get("/me").then().statusCode(401);
    }
}
