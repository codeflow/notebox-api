package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * A tree node resolves through the listings that already exist (FR-09 → C30): one optional
 * {@code group} parameter, and everything else about the listing — order, page defaults, row shape
 * — stays exactly as it shipped (OQ-20, OQ-21, NFR-08).
 */
@QuarkusTest
class GroupFilteredListingTest {

    private static final String TYPE_JSON = """
            {"name":"RabbitMQ","fields":[{"name":"URL","fieldType":"TEXT"}]}""";

    @Inject
    TestData data;

    private String newActorAuth(Tenant tenant) {
        return "Bearer " + TestTokens.valid(UUID.randomUUID(), tenant.getId(), Role.MEMBER.name());
    }

    private String createGroup(String auth, String name, String domain) {
        return given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"" + name + "\", \"domain\": \"" + domain + "\"}")
                .when().post("/groups")
                .then().statusCode(201)
                .extract().path("id");
    }

    private String createType(String auth) {
        return given().header("Authorization", auth).contentType(ContentType.JSON).body(TYPE_JSON)
                .when().post("/annotation-types")
                .then().statusCode(201)
                .extract().path("id");
    }

    private void createRecord(String auth, String typeId, String name, String groupId) {
        String group = groupId == null ? "null" : "\"" + groupId + "\"";
        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"annotationTypeId\":\"" + typeId + "\",\"name\":\"" + name
                        + "\",\"values\":[],\"groupId\":" + group + "}")
                .when().post("/annotation-records")
                .then().statusCode(201);
    }

    private void createTask(String auth, String name, String groupId) {
        String group = groupId == null ? "null" : "\"" + groupId + "\"";
        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "\",\"priority\":\"HIGH\",\"groupId\":" + group + "}")
                .when().post("/tasks")
                .then().statusCode(201);
    }

    @Test
    void filteringATypesRecordsByAGroupNode() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth);
        String alpha = createGroup(auth, "alpha", "ANNOTATION");
        createRecord(auth, typeId, "in-alpha-1", alpha);
        createRecord(auth, typeId, "in-alpha-2", alpha);
        createRecord(auth, typeId, "in-alpha-3", alpha);
        createRecord(auth, typeId, "loose-1", null);
        createRecord(auth, typeId, "loose-2", null);

        given().header("Authorization", auth)
                .when().get("/annotation-records?typeId=" + typeId + "&group=" + alpha)
                .then().statusCode(200)
                .body("items", hasSize(3))
                .body("total", equalTo(3));
    }

    @Test
    void filteringToUngroupedItemsExplicitly() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth);
        String alpha = createGroup(auth, "alpha", "ANNOTATION");
        createRecord(auth, typeId, "in-alpha-1", alpha);
        createRecord(auth, typeId, "in-alpha-2", alpha);
        createRecord(auth, typeId, "in-alpha-3", alpha);
        createRecord(auth, typeId, "loose-1", null);
        createRecord(auth, typeId, "loose-2", null);

        given().header("Authorization", auth)
                .when().get("/annotation-records?typeId=" + typeId + "&group=none")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("total", equalTo(2))
                .body("items.name", equalTo(java.util.List.of("loose-2", "loose-1")));
    }

    @Test
    void filteringTasksByAGroupNode() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String q3 = createGroup(auth, "Q3 Migration", "TASK");
        createTask(auth, "in-q3-1", q3);
        createTask(auth, "in-q3-2", q3);
        createTask(auth, "elsewhere", null);

        given().header("Authorization", auth)
                .when().get("/tasks?group=" + q3)
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("total", equalTo(2))
                .body("items.name", equalTo(java.util.List.of("in-q3-2", "in-q3-1")));
    }

    /** The unfiltered listing is byte-for-byte what it was — the parameter is genuinely optional. */
    @Test
    void omittingTheFilterLeavesTheListingUnchanged() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String q3 = createGroup(auth, "Q3 Migration", "TASK");
        createTask(auth, "first", q3);
        createTask(auth, "second", null);

        given().header("Authorization", auth)
                .when().get("/tasks")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("total", equalTo(2))
                .body("items.name", equalTo(java.util.List.of("second", "first")))
                .body("page", equalTo(0))
                .body("size", equalTo(50));
    }

    /**
     * A filter matching nothing and a filter on something that does not exist are the same
     * observable outcome — which is exactly what keeps it from disclosing existence (C-01).
     */
    @Test
    void anUnknownOrForeignGroupYieldsAnEmptyPageNotA404() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth);
        createRecord(auth, typeId, "loose", null);

        given().header("Authorization", auth)
                .when().get("/annotation-records?typeId=" + typeId + "&group=" + UUID.randomUUID())
                .then().statusCode(200)
                .body("items", hasSize(0))
                .body("total", equalTo(0));

        Tenant theirs = data.createTenant();
        String theirGroup = createGroup(newActorAuth(theirs), "Brokers", "ANNOTATION");

        given().header("Authorization", auth)
                .when().get("/annotation-records?typeId=" + typeId + "&group=" + theirGroup)
                .then().statusCode(200)
                .body("items", hasSize(0))
                .body("total", equalTo(0));
    }

    @Test
    void aMalformedFilterIsRejected() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth);

        given().header("Authorization", auth)
                .when().get("/annotation-records?typeId=" + typeId + "&group=not-a-uuid")
                .then().statusCode(400)
                .body("code", equalTo("group.filter.invalid"));

        given().header("Authorization", auth)
                .when().get("/tasks?group=not-a-uuid")
                .then().statusCode(400)
                .body("code", equalTo("group.filter.invalid"));
    }

    /** total is the pager fact for the FILTERED set, not the whole collection. */
    @Test
    void totalReflectsTheFilteredSetAcrossPages() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String q3 = createGroup(auth, "Q3 Migration", "TASK");
        for (int i = 0; i < 5; i++) {
            createTask(auth, "in-q3-" + i, q3);
        }
        for (int i = 0; i < 4; i++) {
            createTask(auth, "loose-" + i, null);
        }

        given().header("Authorization", auth)
                .when().get("/tasks?group=" + q3 + "&size=2")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("total", equalTo(5));

        given().header("Authorization", auth)
                .when().get("/tasks?group=none&size=2&page=1")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("total", equalTo(4));
    }

    @Test
    void theFilterIsTenantScoped() {
        Tenant mine = data.createTenant();
        String auth = newActorAuth(mine);
        String q3 = createGroup(auth, "Q3 Migration", "TASK");
        createTask(auth, "mine", q3);

        Tenant theirs = data.createTenant();
        String theirAuth = newActorAuth(theirs);

        given().header("Authorization", theirAuth)
                .when().get("/tasks?group=" + q3)
                .then().statusCode(200)
                .body("items", hasSize(0));
    }
}
