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

/**
 * The types listing row (design 07). It is a shape of its own rather than the shared
 * {@link com.notebox.api.api.dto.AnnotationTypeDto}, because a record serialises every component
 * and the single-type responses have no record count to publish.
 */
@QuarkusTest
class AnnotationTypeListingTest {

    @Inject
    TestData data;

    private String tokenFor(Tenant tenant, User user) {
        return "Bearer " + TestTokens.valid(user.getId(), tenant.getId(), "MEMBER");
    }

    private User memberOf(Tenant tenant) {
        return data.createUser(
                tenant.getId(), "tl-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);
    }

    private String createType(String token, String name) {
        return given().header("Authorization", token)
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\",\"fields\":["
                        + "{\"name\":\"Host\",\"fieldType\":\"TEXT\",\"visibleForViewing\":true,"
                        + "\"secret\":false,\"options\":[]},"
                        + "{\"name\":\"Hidden\",\"fieldType\":\"TEXT\",\"visibleForViewing\":false,"
                        + "\"secret\":false,\"options\":[]}]}")
                .when().post("/annotation-types")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    void aRowCountsItsFieldsAndTheVisibleSubsetOfThem() {
        Tenant tenant = data.createTenant();
        User user = memberOf(tenant);
        String token = tokenFor(tenant, user);
        String name = "Type-" + UUID.randomUUID();
        createType(token, name);

        given().header("Authorization", token)
                .when().get("/annotation-types")
                .then().statusCode(200)
                .body("find { it.name == '" + name + "' }.fieldCount", equalTo(2))
                .body("find { it.name == '" + name + "' }.visibleFieldCount", equalTo(1));
    }

    /** A type with no records reports zero — the absence of a count row, not a missing field. */
    @Test
    void aTypeWithNoRecordsReportsZero() {
        Tenant tenant = data.createTenant();
        User user = memberOf(tenant);
        String token = tokenFor(tenant, user);
        String name = "Empty-" + UUID.randomUUID();
        createType(token, name);

        given().header("Authorization", token)
                .when().get("/annotation-types")
                .then().statusCode(200)
                .body("find { it.name == '" + name + "' }.recordCount", equalTo(0));
    }

    @Test
    void aRowCountsTheRecordsOfItsOwnType() {
        Tenant tenant = data.createTenant();
        User user = memberOf(tenant);
        String token = tokenFor(tenant, user);
        String counted = "Counted-" + UUID.randomUUID();
        String other = "Other-" + UUID.randomUUID();
        String countedId = createType(token, counted);
        createType(token, other);

        for (int n = 0; n < 3; n++) {
            given().header("Authorization", token)
                    .contentType("application/json")
                    .body("{\"annotationTypeId\":\"" + countedId + "\",\"name\":\"r" + n + "\",\"values\":[]}")
                    .when().post("/annotation-records").then().statusCode(201);
        }

        given().header("Authorization", token)
                .when().get("/annotation-types")
                .then().statusCode(200)
                .body("find { it.name == '" + counted + "' }.recordCount", equalTo(3))
                // The other type's row must not inherit the count.
                .body("find { it.name == '" + other + "' }.recordCount", equalTo(0));
    }

    /** C-01: the count is the caller's tenant's, never a global total for that type shape. */
    @Test
    void theCountNeverIncludesAnotherTenantsRecords() {
        Tenant mine = data.createTenant();
        Tenant theirs = data.createTenant();
        String myToken = tokenFor(mine, memberOf(mine));
        String theirToken = tokenFor(theirs, memberOf(theirs));

        String name = "Shared-" + UUID.randomUUID();
        createType(myToken, name);
        String theirTypeId = createType(theirToken, name);
        given().header("Authorization", theirToken)
                .contentType("application/json")
                .body("{\"annotationTypeId\":\"" + theirTypeId + "\",\"name\":\"theirs\",\"values\":[]}")
                .when().post("/annotation-records").then().statusCode(201);

        given().header("Authorization", myToken)
                .when().get("/annotation-types")
                .then().statusCode(200)
                .body("find { it.name == '" + name + "' }.recordCount", equalTo(0));
    }
}
