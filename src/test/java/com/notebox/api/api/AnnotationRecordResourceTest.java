package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

/** End-to-end annotation-record CRUD + reveal: masking, role gate and tenant isolation (FR-04/06/18, C-01/02/03). */
@QuarkusTest
class AnnotationRecordResourceTest {

    private static final String TYPE_JSON = """
            {"name":"RabbitMQ","fields":[
              {"name":"URL","fieldType":"TEXT"},
              {"name":"API key","fieldType":"TEXT","secret":true}
            ]}""";

    @Inject
    TestData data;

    private String token(Tenant tenant, Role role) {
        return TestTokens.valid(UUID.randomUUID(), tenant.getId(), role.name());
    }

    private Response createType(String token) {
        return given().header("Authorization", "Bearer " + token)
                .contentType("application/json").body(TYPE_JSON)
                .when().post("/annotation-types")
                .then().statusCode(201)
                .extract().response();
    }

    private String recordJson(String typeId, String urlFieldId, String apiKeyFieldId) {
        return """
                {"annotationTypeId":"%s","name":"prod-broker","values":[
                  {"fieldId":"%s","text":"amqp://h"},
                  {"fieldId":"%s","text":"s3cr3t-token"}
                ]}""".formatted(typeId, urlFieldId, apiKeyFieldId);
    }

    private String createRecord(String token, String typeId, String urlFieldId, String apiKeyFieldId) {
        return given().header("Authorization", "Bearer " + token)
                .contentType("application/json").body(recordJson(typeId, urlFieldId, apiKeyFieldId))
                .when().post("/annotation-records")
                .then().statusCode(201)
                .body("name", equalTo("prod-broker"))
                .extract().path("id");
    }

    @Test
    void create_thenReadMasksTheSecretValue() {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = createType(token);
        String typeId = type.jsonPath().getString("id");
        String urlFieldId = type.jsonPath().getString("fields[0].id");
        String apiKeyFieldId = type.jsonPath().getString("fields[1].id");

        String recordId = createRecord(token, typeId, urlFieldId, apiKeyFieldId);

        String body = given().header("Authorization", "Bearer " + token)
                .when().get("/annotation-records/" + recordId)
                .then().statusCode(200)
                .body("values.find { it.fieldId == '" + apiKeyFieldId + "' }.masked", equalTo(true))
                .body("values.find { it.fieldId == '" + apiKeyFieldId + "' }.text", nullValue())
                .body("values.find { it.fieldId == '" + urlFieldId + "' }.text", equalTo("amqp://h"))
                .extract().asString();
        assertFalse(body.contains("s3cr3t-token"), "no corner of the read body may carry the cleartext (audit F10)");
    }

    @Test
    void duplicateValueForTheSameFieldIsALocalized400NotA500() {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = createType(token);
        String typeId = type.jsonPath().getString("id");
        String urlFieldId = type.jsonPath().getString("fields[0].id");

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"annotationTypeId":"%s","name":"dup","values":[
                          {"fieldId":"%s","text":"a"},
                          {"fieldId":"%s","text":"b"}
                        ]}""".formatted(typeId, urlFieldId, urlFieldId))
                .when().post("/annotation-records")
                .then().statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("violations.code", hasItem("annotation.record.value.duplicate_field"));
    }

    @Test
    void nameOver120CharsIsALocalized400NotA500() {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = createType(token);
        String typeId = type.jsonPath().getString("id");

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"annotationTypeId\":\"" + typeId + "\",\"name\":\"" + "x".repeat(121)
                        + "\",\"values\":[]}")
                .when().post("/annotation-records")
                .then().statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("violations.code", hasItem("annotation.record.name.too_long"));
    }

    @Test
    void secretValueOverTheColumnBoundIsALocalized400NotA500() {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = createType(token);
        String typeId = type.jsonPath().getString("id");
        String apiKeyFieldId = type.jsonPath().getString("fields[1].id");

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"annotationTypeId":"%s","name":"too-long","values":[
                          {"fieldId":"%s","text":"%s"}
                        ]}""".formatted(typeId, apiKeyFieldId, "x".repeat(4_081)))
                .when().post("/annotation-records")
                .then().statusCode(400)
                .body("code", equalTo("annotation.record.value.too_long"));
    }

    @Test
    void boundaryLengthValuesAreAccepted() {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = createType(token);
        String typeId = type.jsonPath().getString("id");
        String urlFieldId = type.jsonPath().getString("fields[0].id");
        String apiKeyFieldId = type.jsonPath().getString("fields[1].id");

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"annotationTypeId":"%s","name":"%s","values":[
                          {"fieldId":"%s","text":"%s"},
                          {"fieldId":"%s","text":"%s"}
                        ]}""".formatted(typeId, "n".repeat(120),
                        urlFieldId, "t".repeat(65_535),
                        apiKeyFieldId, "s".repeat(4_080)))
                .when().post("/annotation-records")
                .then().statusCode(201);
    }

    @Test
    void nullValueElementIsALocalized400NotA500() {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = createType(token);
        String typeId = type.jsonPath().getString("id");

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"annotationTypeId\":\"" + typeId + "\",\"name\":\"x\",\"values\":[null]}")
                .when().post("/annotation-records")
                .then().statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("violations.code", hasItem("annotation.record.value.required"));
    }

    @Test
    void unknownFieldErrorIsLocalizedToTheCallersLocale() {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = createType(token);
        String typeId = type.jsonPath().getString("id");

        given().header("Authorization", "Bearer " + token)
                .header("Accept-Language", "pt")
                .contentType("application/json")
                .body("""
                        {"annotationTypeId":"%s","name":"x","values":[
                          {"fieldId":"%s","text":"y"}
                        ]}""".formatted(typeId, UUID.randomUUID()))
                .when().post("/annotation-records")
                .then().statusCode(400)
                .body("code", equalTo("annotation.record.field.unknown"))
                .body("message", equalTo("Este valor se refere a um campo que o tipo de anotação não define."));
    }

    @Test
    void putWithClearSecretErasesTheSecretExplicitly() {
        Tenant tenant = data.createTenant();
        String memberToken = token(tenant, Role.MEMBER);
        String adminToken = token(tenant, Role.ADMIN);
        Response type = createType(memberToken);
        String typeId = type.jsonPath().getString("id");
        String urlFieldId = type.jsonPath().getString("fields[0].id");
        String apiKeyFieldId = type.jsonPath().getString("fields[1].id");
        String recordId = createRecord(memberToken, typeId, urlFieldId, apiKeyFieldId);

        given().header("Authorization", "Bearer " + memberToken)
                .contentType("application/json")
                .body("""
                        {"name":"prod-broker","values":[
                          {"fieldId":"%s","text":"amqp://h"},
                          {"fieldId":"%s","clearSecret":true}
                        ]}""".formatted(urlFieldId, apiKeyFieldId))
                .when().put("/annotation-records/" + recordId)
                .then().statusCode(200);

        given().header("Authorization", "Bearer " + adminToken)
                .when().post("/annotation-records/" + recordId + "/values/" + apiKeyFieldId + "/reveal")
                .then().statusCode(400)
                .body("code", equalTo("annotation.record.reveal.not_secret"));
    }

    @Test
    void reveal_returnsCleartextToAdminAndIsForbiddenToAMember() {
        Tenant tenant = data.createTenant();
        String memberToken = token(tenant, Role.MEMBER);
        String adminToken = token(tenant, Role.ADMIN);
        Response type = createType(memberToken);
        String typeId = type.jsonPath().getString("id");
        String urlFieldId = type.jsonPath().getString("fields[0].id");
        String apiKeyFieldId = type.jsonPath().getString("fields[1].id");
        String recordId = createRecord(memberToken, typeId, urlFieldId, apiKeyFieldId);

        given().header("Authorization", "Bearer " + adminToken)
                .when().post("/annotation-records/" + recordId + "/values/" + apiKeyFieldId + "/reveal")
                .then().statusCode(200)
                .body("value", equalTo("s3cr3t-token"));

        given().header("Authorization", "Bearer " + memberToken)
                .when().post("/annotation-records/" + recordId + "/values/" + apiKeyFieldId + "/reveal")
                .then().statusCode(403)
                .body("code", equalTo("annotation.record.secret.reveal.forbidden"));
    }

    @Test
    void blankNameIsRejectedWithLocalizedViolation() {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = createType(token);
        String typeId = type.jsonPath().getString("id");

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"annotationTypeId\":\"" + typeId + "\",\"name\":\"\",\"values\":[]}")
                .when().post("/annotation-records")
                .then().statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("violations.code", hasItem("annotation.record.name.required"));
    }

    @Test
    void foreignTenantCannotCreateAgainstAnotherTenantsType() {
        Tenant tenantB = data.createTenant();
        String tokenB = token(tenantB, Role.MEMBER);
        Response typeB = createType(tokenB);
        String typeBId = typeB.jsonPath().getString("id");

        Tenant tenantA = data.createTenant();
        String tokenA = token(tenantA, Role.MEMBER);

        given().header("Authorization", "Bearer " + tokenA)
                .contentType("application/json")
                .body("{\"annotationTypeId\":\"" + typeBId + "\",\"name\":\"x\",\"values\":[]}")
                .when().post("/annotation-records")
                .then().statusCode(404);
    }

    @Test
    void foreignTenantCannotReadUpdateOrDeleteARecord() {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = createType(token);
        String typeId = type.jsonPath().getString("id");
        String urlFieldId = type.jsonPath().getString("fields[0].id");
        String apiKeyFieldId = type.jsonPath().getString("fields[1].id");
        String recordId = createRecord(token, typeId, urlFieldId, apiKeyFieldId);

        String otherToken = token(data.createTenant(), Role.MEMBER);

        given().header("Authorization", "Bearer " + otherToken)
                .when().get("/annotation-records/" + recordId)
                .then().statusCode(404)
                .body("code", equalTo("annotation.record.not_found"));

        given().header("Authorization", "Bearer " + otherToken)
                .contentType("application/json")
                .body("{\"name\":\"renamed\",\"values\":[]}")
                .when().put("/annotation-records/" + recordId)
                .then().statusCode(404);

        given().header("Authorization", "Bearer " + otherToken)
                .when().delete("/annotation-records/" + recordId)
                .then().statusCode(404);
    }

    @Test
    void unauthenticatedIsRejected() {
        given().when().get("/annotation-records/" + UUID.randomUUID())
                .then().statusCode(401);
    }

    // --- feat-007: C-08 at the wire — write, read and the legacy-row seam ---

    @jakarta.inject.Inject
    jakarta.persistence.EntityManager em;

    @jakarta.inject.Inject
    jakarta.transaction.UserTransaction utx;

    private static final String NOTES_TYPE_JSON = """
            {"name":"Runbook","fields":[{"name":"Notes","fieldType":"FREE_TEXT"}]}""";

    @Test
    void create_sanitizesHostileRichTextOnTheWire() {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = given().header("Authorization", "Bearer " + token)
                .contentType("application/json").body(NOTES_TYPE_JSON)
                .when().post("/annotation-types")
                .then().statusCode(201).extract().response();
        String typeId = type.jsonPath().getString("id");
        String notesId = type.jsonPath().getString("fields[0].id");

        String recordId = given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"annotationTypeId":"%s","name":"r1","values":[
                          {"fieldId":"%s","text":"<p>before</p><script>steal()</script><p onclick=\\"x()\\">after</p>"}]}"""
                        .formatted(typeId, notesId))
                .when().post("/annotation-records")
                .then().statusCode(201)
                .body("values[0].text", equalTo("<p>before</p><p>after</p>"))
                .extract().path("id");

        given().header("Authorization", "Bearer " + token)
                .when().get("/annotation-records/" + recordId)
                .then().statusCode(200)
                .body("values[0].text", equalTo("<p>before</p><p>after</p>"));
    }

    @Test
    void legacyHostileRow_isServedInert_andTheStoredBytesAreUntouched() throws Exception {
        Tenant tenant = data.createTenant();
        String token = token(tenant, Role.MEMBER);
        Response type = given().header("Authorization", "Bearer " + token)
                .contentType("application/json").body(NOTES_TYPE_JSON)
                .when().post("/annotation-types")
                .then().statusCode(201).extract().response();
        String typeId = type.jsonPath().getString("id");
        String notesId = type.jsonPath().getString("fields[0].id");
        String recordId = given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"annotationTypeId":"%s","name":"r1","values":[
                          {"fieldId":"%s","text":"<p>ok</p>"}]}""".formatted(typeId, notesId))
                .when().post("/annotation-records")
                .then().statusCode(201).extract().path("id");

        // A row written before feat-007 existed: hostile bytes planted straight into the store.
        String hostile = "<p>x</p><script>steal()</script>";
        utx.begin();
        em.createNativeQuery("update annotation_value set text_value = :v where annotation_record_id = :id")
                .setParameter("v", hostile)
                .setParameter("id", recordId)
                .executeUpdate();
        utx.commit();

        given().header("Authorization", "Bearer " + token)
                .when().get("/annotation-records/" + recordId)
                .then().statusCode(200)
                .body("values[0].text", equalTo("<p>x</p>"));

        Object stored = em.createNativeQuery(
                        "select text_value from annotation_value where annotation_record_id = :id")
                .setParameter("id", recordId)
                .getSingleResult();
        assertEquals(hostile, stored,
                "the read is DTO-only — the transactional GET must not flush a sanitized UPDATE (INV-S5)");
    }
}
