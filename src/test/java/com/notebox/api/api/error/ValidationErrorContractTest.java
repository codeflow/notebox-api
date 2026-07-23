package com.notebox.api.api.error;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Guards the unified validation contract across both error styles: feat-001 endpoints (whose DTOs
 * still carry Bean Validation's default messages) must degrade to a clean 400 — never a 500 — while
 * feat-003 endpoints (dot-namespaced keys) return localized per-field violations (F1 regression guard).
 */
@QuarkusTest
class ValidationErrorContractTest {

    @Inject
    TestData data;

    @Test
    void feat001LoginWithInvalidInput_returns400NotServerError() {
        given().contentType("application/json")
                .body("{\"email\":\"not-an-email\",\"password\":\"\"}")
                .when().post("/auth/login")
                .then().statusCode(400)
                .body("code", equalTo("validation.failed"));
    }

    @Test
    void feat003InvalidFieldName_returnsLocalizedDotKeyViolation() {
        Tenant tenant = data.createTenant();
        String token = TestTokens.valid(UUID.randomUUID(), tenant.getId(), Role.MEMBER.name());

        given().header("Authorization", "Bearer " + token).contentType("application/json")
                .body("{\"name\":\"X\",\"fields\":[{\"name\":\"\",\"fieldType\":\"TEXT\"}]}")
                .when().post("/annotation-types")
                .then().statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("violations.code", hasItem("annotation.field.name.required"));
    }
}
