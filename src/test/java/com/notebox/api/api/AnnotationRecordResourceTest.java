package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;

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

        given().header("Authorization", "Bearer " + token)
                .when().get("/annotation-records/" + recordId)
                .then().statusCode(200)
                .body("values.find { it.fieldId == '" + apiKeyFieldId + "' }.masked", equalTo(true))
                .body("values.find { it.fieldId == '" + apiKeyFieldId + "' }.text", nullValue())
                .body("values.find { it.fieldId == '" + urlFieldId + "' }.text", equalTo("amqp://h"));
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
}
