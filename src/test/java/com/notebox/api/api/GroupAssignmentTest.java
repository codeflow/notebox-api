package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

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
 * Assigning an item to a group at the wire (FR-08): at most one group, of the item's own namespace,
 * set and cleared through the established replace-update contract.
 */
@QuarkusTest
class GroupAssignmentTest {

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

    private static String recordJson(String typeId, String name, String groupId) {
        String group = groupId == null ? "null" : "\"" + groupId + "\"";
        return "{\"annotationTypeId\":\"" + typeId + "\",\"name\":\"" + name
                + "\",\"values\":[],\"groupId\":" + group + "}";
    }

    private static String recordJsonWithoutGroup(String typeId, String name) {
        return "{\"annotationTypeId\":\"" + typeId + "\",\"name\":\"" + name + "\",\"values\":[]}";
    }

    private static String taskJson(String name, String groupId) {
        String group = groupId == null ? "null" : "\"" + groupId + "\"";
        return "{\"name\":\"" + name + "\",\"priority\":\"HIGH\",\"groupId\":" + group + "}";
    }

    @Test
    void assigningARecordToAnAnnotationGroup() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth);
        String groupId = createGroup(auth, "Brokers", "ANNOTATION");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(recordJson(typeId, "prod-broker", groupId))
                .when().post("/annotation-records")
                .then().statusCode(201)
                .body("groupId", equalTo(groupId));
    }

    @Test
    void assigningATaskToATaskGroup() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String groupId = createGroup(auth, "Q3 Migration", "TASK");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskJson("Migrate broker", groupId))
                .when().post("/tasks")
                .then().statusCode(201)
                .body("groupId", equalTo(groupId));
    }

    /** At most one — a second assignment replaces, it never accumulates. */
    @Test
    void aSecondAssignmentReplacesTheFirst() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth);
        String brokers = createGroup(auth, "Brokers", "ANNOTATION");
        String datastores = createGroup(auth, "Datastores", "ANNOTATION");

        String recordId = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(recordJson(typeId, "prod-broker", brokers))
                .when().post("/annotation-records")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(recordJson(typeId, "prod-broker", datastores))
                .when().put("/annotation-records/" + recordId)
                .then().statusCode(200)
                .body("groupId", equalTo(datastores));
    }

    /**
     * The PUT-replace hazard, made explicit: an update that omits groupId CLEARS it, exactly as an
     * omitted card or details clears those. A legacy client that does not send the field un-groups
     * the item (plan Risk 1) — which is why feat-014 and feat-015 promote together.
     */
    @Test
    void omittingTheGroupOnUpdateClearsIt() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth);
        String groupId = createGroup(auth, "Brokers", "ANNOTATION");

        String recordId = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(recordJson(typeId, "prod-broker", groupId))
                .when().post("/annotation-records")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(recordJsonWithoutGroup(typeId, "prod-broker"))
                .when().put("/annotation-records/" + recordId)
                .then().statusCode(200)
                .body("groupId", is(nullValue()));

        given().header("Authorization", auth)
                .when().get("/groups/" + groupId)
                .then().statusCode(200);
    }

    @Test
    void omittingTheGroupOnATaskUpdateClearsIt() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String groupId = createGroup(auth, "Q3 Migration", "TASK");

        String taskId = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskJson("Migrate broker", groupId))
                .when().post("/tasks")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\":\"Migrate broker\",\"priority\":\"HIGH\"}")
                .when().put("/tasks/" + taskId)
                .then().statusCode(200)
                .body("groupId", is(nullValue()));
    }

    /** I-8 — the rule no foreign key can express, on the task side. */
    @Test
    void aTaskCannotBePutInAnAnnotationGroup() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String annotationGroup = createGroup(auth, "Brokers", "ANNOTATION");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskJson("Migrate broker", annotationGroup))
                .when().post("/tasks")
                .then().statusCode(409)
                .body("code", equalTo("group.domain.mismatch"));
    }

    /** And the mirror, on the record side — so the two surfaces cannot drift apart. */
    @Test
    void aRecordCannotBePutInATaskGroup() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth);
        String taskGroup = createGroup(auth, "Q3 Migration", "TASK");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(recordJson(typeId, "prod-broker", taskGroup))
                .when().post("/annotation-records")
                .then().statusCode(409)
                .body("code", equalTo("group.domain.mismatch"));
    }

    /** C-01: a foreign tenant's group is exactly a group that does not exist. */
    @Test
    void aForeignTenantsGroupIsIndistinguishableFromAMissingOne() {
        Tenant theirTenant = data.createTenant();
        String theirGroup = createGroup(newActorAuth(theirTenant), "Brokers", "ANNOTATION");

        Tenant myTenant = data.createTenant();
        String auth = newActorAuth(myTenant);
        String typeId = createType(auth);

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(recordJson(typeId, "prod-broker", theirGroup))
                .when().post("/annotation-records")
                .then().statusCode(404)
                .body("code", equalTo("group.not_found"));

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(recordJson(typeId, "prod-broker", UUID.randomUUID().toString()))
                .when().post("/annotation-records")
                .then().statusCode(404)
                .body("code", equalTo("group.not_found"));
    }

    /** A record created without a group is ungrouped, and stays that way on read. */
    @Test
    void anItemCreatedWithoutAGroupIsUngrouped() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth);

        String recordId = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(recordJsonWithoutGroup(typeId, "loose-record"))
                .when().post("/annotation-records")
                .then().statusCode(201)
                .body("groupId", is(nullValue()))
                .extract().path("id");

        given().header("Authorization", auth)
                .when().get("/annotation-records/" + recordId)
                .then().statusCode(200)
                .body("groupId", is(nullValue()));
    }

    /** The listing row carries the group too, so a grid can show it without a second read. */
    @Test
    void theTaskListingRowCarriesTheGroup() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String groupId = createGroup(auth, "Q3 Migration", "TASK");
        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskJson("Migrate broker", groupId))
                .when().post("/tasks")
                .then().statusCode(201);

        given().header("Authorization", auth)
                .when().get("/tasks")
                .then().statusCode(200)
                .body("items[0].groupId", equalTo(groupId));
    }
}
