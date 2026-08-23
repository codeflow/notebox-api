package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

/**
 * The navigation tree at the wire (FR-09): composition (OQ-23), ordering with Ungrouped pinned last
 * (OQ-25), the structural-vs-projection presence rule, and the standing tenant guarantees.
 */
@QuarkusTest
class NavigationResourceTest {

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

    private String createType(String auth, String name) {
        return given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "\",\"fields\":[{\"name\":\"URL\",\"fieldType\":\"TEXT\"}]}")
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

    private Response tree(String auth) {
        return given().header("Authorization", auth)
                .when().get("/navigation")
                .then().statusCode(200)
                .extract().response();
    }

    @Test
    void theTreeCarriesTypesUnderAnnotationsAndGroupsUnderEachType() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String rabbit = createType(auth, "RabbitMQ");
        String kafka = createType(auth, "Kafka");
        String alpha = createGroup(auth, "alpha", "ANNOTATION");
        String gamma = createGroup(auth, "gamma", "ANNOTATION");
        createRecord(auth, rabbit, "r-alpha", alpha);
        createRecord(auth, rabbit, "r-gamma", gamma);
        createRecord(auth, rabbit, "r-loose", null);
        createRecord(auth, kafka, "k-alpha", alpha);
        String q3 = createGroup(auth, "Q3 Migration", "TASK");
        createTask(auth, "t-in-q3", q3);
        createTask(auth, "t-loose", null);

        Response tree = tree(auth);

        assertTypeOrder(tree, List.of("Kafka", "RabbitMQ"));
        assertEquals(tree, "annotations[1].typeId", rabbit);
        assertEquals(tree, "annotations[1].groups[0].name", "alpha");
        assertEquals(tree, "annotations[1].groups[1].name", "gamma");
        given().header("Authorization", auth).when().get("/navigation").then()
                .body("annotations[1].groups", hasSize(3))
                .body("annotations[1].groups[2].groupId", is(nullValue()))
                .body("annotations[1].groups[2].name", is(nullValue()))
                // Kafka has one grouped record and no ungrouped one -> no Ungrouped node
                .body("annotations[0].groups", hasSize(1))
                .body("annotations[0].groups[0].name", equalTo("alpha"))
                .body("tasks", hasSize(2))
                .body("tasks[0].name", equalTo("Q3 Migration"))
                .body("tasks[1].groupId", is(nullValue()));
    }

    /** OQ-25: case-insensitive name order, and Ungrouped after every named group. */
    @Test
    void siblingOrderingIsCaseInsensitiveWithUngroupedLast() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth, "RabbitMQ");
        String gamma = createGroup(auth, "gamma", "ANNOTATION");
        String beta = createGroup(auth, "Beta", "ANNOTATION");
        String alpha = createGroup(auth, "alpha", "ANNOTATION");
        createRecord(auth, typeId, "r1", gamma);
        createRecord(auth, typeId, "r2", beta);
        createRecord(auth, typeId, "r3", alpha);
        createRecord(auth, typeId, "r4", null);

        given().header("Authorization", auth)
                .when().get("/navigation")
                .then().statusCode(200)
                .body("annotations[0].groups.name",
                        equalTo(java.util.Arrays.asList("alpha", "Beta", "gamma", null)))
                .body("annotations[0].groups[3].groupId", is(nullValue()));
    }

    /** A type node is structural; a group node is a projection of real membership. */
    @Test
    void aTypeNodeIsStructuralAGroupNodeIsAProjection() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        createType(auth, "Postgres");
        createGroup(auth, "empty-group", "ANNOTATION");

        given().header("Authorization", auth)
                .when().get("/navigation")
                .then().statusCode(200)
                .body("annotations", hasSize(1))
                .body("annotations[0].name", equalTo("Postgres"))
                .body("annotations[0].groups", is(empty()));

        // ...but the unused group is still listed for assignment UIs
        given().header("Authorization", auth)
                .when().get("/groups?domain=ANNOTATION")
                .then().statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].name", equalTo("empty-group"));
    }

    @Test
    void aTenantWithNothingGetsBothRootsEmpty() {
        Tenant tenant = data.createTenant();

        given().header("Authorization", newActorAuth(tenant))
                .when().get("/navigation")
                .then().statusCode(200)
                .body("annotations", is(empty()))
                .body("tasks", is(empty()));
    }

    /** The tree never enumerates a record or a task — OQ-23's whole point (NFR-08). */
    @Test
    void theTreeStopsAtGroupNodes() {
        Tenant tenant = data.createTenant();
        String auth = newActorAuth(tenant);
        String typeId = createType(auth, "RabbitMQ");
        String alpha = createGroup(auth, "alpha", "ANNOTATION");
        createRecord(auth, typeId, "a-very-distinctive-record-name", alpha);
        createTask(auth, "a-very-distinctive-task-name", null);

        String body = tree(auth).asString();

        org.junit.jupiter.api.Assertions.assertFalse(
                body.contains("a-very-distinctive-record-name"), "no record appears in the tree");
        org.junit.jupiter.api.Assertions.assertFalse(
                body.contains("a-very-distinctive-task-name"), "no task appears in the tree");
    }

    /** NFR-01: nothing of another tenant's leaks into the tree. */
    @Test
    void theTreeNeverCrossesTenants() {
        Tenant theirs = data.createTenant();
        String theirAuth = newActorAuth(theirs);
        String theirType = createType(theirAuth, "TheirSecretType");
        String theirGroup = createGroup(theirAuth, "TheirSecretGroup", "ANNOTATION");
        createRecord(theirAuth, theirType, "theirs", theirGroup);
        createTask(theirAuth, "their-task", null);

        Tenant mine = data.createTenant();
        String body = tree(newActorAuth(mine)).asString();

        org.junit.jupiter.api.Assertions.assertFalse(body.contains("TheirSecretType"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("TheirSecretGroup"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains(theirGroup));
    }

    /** C-02. */
    @Test
    void theNavigationEndpointRefusesAnUnauthenticatedCaller() {
        given().when().get("/navigation").then().statusCode(401);
    }

    private static void assertTypeOrder(Response tree, List<String> expected) {
        org.junit.jupiter.api.Assertions.assertEquals(
                expected, tree.jsonPath().getList("annotations.name"));
    }

    private static void assertEquals(Response tree, String path, String expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, tree.jsonPath().getString(path));
    }
}
