package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * The group endpoints at the wire (FR-08): the full validation matrix, the per-namespace listing,
 * and the standing guarantees — authenticated by default (C-02), a foreign tenant indistinguishable
 * from a missing id (C-01), and localized messages (C-09).
 */
@QuarkusTest
class GroupResourceTest {

    @Inject
    TestData data;

    private String newActorAuth() {
        Tenant tenant = data.createTenant();
        User member = data.createUser(
                tenant.getId(), UUID.randomUUID() + "@groups.test", "pw-wire", Role.MEMBER);
        return "Bearer " + TestTokens.valid(member.getId(), tenant.getId(), "MEMBER");
    }

    private static String groupJson(String name, String domain) {
        return "{\"name\": \"" + name + "\", \"domain\": \"" + domain + "\"}";
    }

    private String createGroup(String auth, String name, String domain) {
        return given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(groupJson(name, domain))
                .when().post("/groups")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    void createsAGroupInEachNamespace() {
        String auth = newActorAuth();

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(groupJson("Brokers", "ANNOTATION"))
                .when().post("/groups")
                .then().statusCode(201)
                .body("name", equalTo("Brokers"))
                .body("domain", equalTo("ANNOTATION"));

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(groupJson("Q3 Migration", "TASK"))
                .when().post("/groups")
                .then().statusCode(201)
                .body("domain", equalTo("TASK"));
    }

    @Test
    void theSameNameIsFreeInTheOtherDomainButTakenInItsOwn() {
        String auth = newActorAuth();
        createGroup(auth, "Brokers", "ANNOTATION");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(groupJson("Brokers", "TASK"))
                .when().post("/groups")
                .then().statusCode(201);

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(groupJson("Brokers", "ANNOTATION"))
                .when().post("/groups")
                .then().statusCode(409)
                .body("code", equalTo("group.name.duplicate"));
    }

    /**
     * OQ-04 fixed groups as flat. NOTE a deviation from the spec's wording: the scenario says the
     * request "is rejected", but unknown JSON properties are ignored project-wide (no
     * FAIL_ON_UNKNOWN_PROPERTIES), so a stated parent yields 201 and is dropped. The substance
     * holds — nesting is unrepresentable, no parent is stored or returned — but the literal
     * rejection does not happen. Making it reject would be a global deserialization change
     * affecting every endpoint, which is outside this task. Raised for the audit.
     */
    @Test
    void aGroupCannotBeNested() {
        String auth = newActorAuth();
        String parentId = createGroup(auth, "Parent", "ANNOTATION");

        String childId = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"Child\", \"domain\": \"ANNOTATION\", \"parentId\": \"" + parentId + "\"}")
                .when().post("/groups")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", auth)
                .when().get("/groups/" + childId)
                .then().statusCode(200)
                .body("parentId", is(nullValue()))
                .body("parent", is(nullValue()));
    }

    @Test
    void aBlankNameIsRejected() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(groupJson("", "ANNOTATION"))
                .when().post("/groups")
                .then().statusCode(400)
                .body("violations.find { it.code == 'group.name.required' }.code",
                        equalTo("group.name.required"));
    }

    @Test
    void aNameAbove120CharactersIsRejected() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(groupJson("g".repeat(121), "ANNOTATION"))
                .when().post("/groups")
                .then().statusCode(400)
                .body("violations.find { it.code == 'group.name.too_long' }.code",
                        equalTo("group.name.too_long"));
    }

    @Test
    void aMissingDomainIsRejected() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body("{\"name\": \"Brokers\"}")
                .when().post("/groups")
                .then().statusCode(400)
                .body("violations.find { it.code == 'group.domain.required' }.code",
                        equalTo("group.domain.required"));
    }

    @Test
    void aDomainOutsideTheClosedSetIsRejected() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(groupJson("Brokers", "SOMETHING_ELSE"))
                .when().post("/groups")
                .then().statusCode(400)
                .body("violations.find { it.code == 'group.domain.invalid' }.code",
                        equalTo("group.domain.invalid"));
    }

    @Test
    void statingADifferentDomainOnReplaceIsRejected() {
        String auth = newActorAuth();
        String id = createGroup(auth, "Brokers", "ANNOTATION");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(groupJson("Brokers", "TASK"))
                .when().put("/groups/" + id)
                .then().statusCode(409)
                .body("code", equalTo("group.domain.not_modifiable"));

        given().header("Authorization", auth)
                .when().get("/groups/" + id)
                .then().statusCode(200)
                .body("domain", equalTo("ANNOTATION"));
    }

    @Test
    void renamesAGroup() {
        String auth = newActorAuth();
        String id = createGroup(auth, "Brokers", "ANNOTATION");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(groupJson("Message Brokers", "ANNOTATION"))
                .when().put("/groups/" + id)
                .then().statusCode(200)
                .body("name", equalTo("Message Brokers"));
    }

    @Test
    void deletesAGroup() {
        String auth = newActorAuth();
        String id = createGroup(auth, "Brokers", "ANNOTATION");

        given().header("Authorization", auth)
                .when().delete("/groups/" + id)
                .then().statusCode(204);

        given().header("Authorization", auth)
                .when().get("/groups/" + id)
                .then().statusCode(404)
                .body("code", equalTo("group.not_found"));
    }

    @Test
    void listingIsPerNamespaceAndNameOrdered() {
        String auth = newActorAuth();
        createGroup(auth, "gamma", "ANNOTATION");
        createGroup(auth, "Beta", "ANNOTATION");
        createGroup(auth, "alpha", "ANNOTATION");
        createGroup(auth, "Q3 Migration", "TASK");

        given().header("Authorization", auth)
                .when().get("/groups?domain=ANNOTATION")
                .then().statusCode(200)
                .body("items", hasSize(3))
                .body("total", equalTo(3))
                .body("items.name", equalTo(java.util.List.of("alpha", "Beta", "gamma")));

        given().header("Authorization", auth)
                .when().get("/groups?domain=TASK")
                .then().statusCode(200)
                .body("items", hasSize(1));
    }

    @Test
    void listingWithoutADomainIsRejected() {
        given().header("Authorization", newActorAuth())
                .when().get("/groups")
                .then().statusCode(400);
    }

    /**
     * The listing's domain parameter is validated, not just deserialized: a bogus value must be a
     * localized 400, never an IllegalArgumentException from valueOf surfacing as a 500.
     */
    @Test
    void listingRejectsADomainOutsideTheClosedSet() {
        given().header("Authorization", newActorAuth())
                .when().get("/groups?domain=SOMETHING_ELSE")
                .then().statusCode(400);
    }

    @Test
    void listingRejectsAnOutOfBoundsPageSize() {
        given().header("Authorization", newActorAuth())
                .when().get("/groups?domain=ANNOTATION&size=201")
                .then().statusCode(400);
    }

    /** C-01: another tenant's group is exactly a group that does not exist. */
    @Test
    void aForeignTenantsGroupIsIndistinguishableFromAMissingOne() {
        String theirs = createGroup(newActorAuth(), "Brokers", "ANNOTATION");
        String mine = newActorAuth();

        given().header("Authorization", mine)
                .when().get("/groups/" + theirs)
                .then().statusCode(404).body("code", equalTo("group.not_found"));

        given().header("Authorization", mine).contentType(ContentType.JSON)
                .body(groupJson("Renamed", "ANNOTATION"))
                .when().put("/groups/" + theirs)
                .then().statusCode(404);

        given().header("Authorization", mine)
                .when().delete("/groups/" + theirs)
                .then().statusCode(404);
    }

    /** C-02: no group endpoint answers an unauthenticated caller. */
    @Test
    void everyGroupEndpointRefusesAnUnauthenticatedCaller() {
        String id = UUID.randomUUID().toString();

        given().when().get("/groups?domain=ANNOTATION").then().statusCode(401);
        given().contentType(ContentType.JSON).body(groupJson("Brokers", "ANNOTATION"))
                .when().post("/groups").then().statusCode(401);
        given().when().get("/groups/" + id).then().statusCode(401);
        given().contentType(ContentType.JSON).body(groupJson("Brokers", "ANNOTATION"))
                .when().put("/groups/" + id).then().statusCode(401);
        given().when().delete("/groups/" + id).then().statusCode(401);
    }

    /** C-09: the duplicate-name rejection reaches a pt caller in Portuguese. */
    @Test
    void validationMessage_resolvesInPortugueseLocale() {
        String auth = newActorAuth();
        createGroup(auth, "Brokers", "ANNOTATION");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .header("Accept-Language", "pt")
                .body(groupJson("Brokers", "ANNOTATION"))
                .when().post("/groups")
                .then().statusCode(409)
                .body("message", equalTo("Já existe um grupo com este nome neste domínio."));
    }

    @Test
    void blankNameMessage_resolvesInPortugueseLocale() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .header("Accept-Language", "pt")
                .body(groupJson("", "ANNOTATION"))
                .when().post("/groups")
                .then().statusCode(400)
                .body("violations.find { it.code == 'group.name.required' }.message",
                        equalTo("O nome do grupo é obrigatório."));
    }
}
