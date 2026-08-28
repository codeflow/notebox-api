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
 * The subtask count on the task listing (design 15). The listing deliberately does not load
 * subtasks, so this comes from a grouped count for the page — the property worth pinning is that it
 * is right, and that a task without subtasks reports zero rather than nothing.
 */
@QuarkusTest
class TaskSubtaskCountTest {

    @Inject
    TestData data;

    private String tokenFor(Tenant tenant) {
        User user = data.createUser(
                tenant.getId(), "sc-" + UUID.randomUUID() + "@notebox.test", "s3cret-pass", Role.MEMBER);
        return "Bearer " + TestTokens.valid(user.getId(), tenant.getId(), "MEMBER");
    }

    /** Subtasks have their own endpoint — TaskInput carries none, so they are added after creation. */
    private String createTask(String token, String name, int subtasks) {
        String id = given().header("Authorization", token)
                .contentType("application/json")
                .body("{\"name\":\"" + name + "\",\"priority\":\"HIGH\"}")
                .when().post("/tasks").then().statusCode(201)
                .extract().path("id");
        for (int n = 0; n < subtasks; n++) {
            given().header("Authorization", token)
                    .contentType("application/json")
                    .body("{\"name\":\"s" + n + "\",\"done\":false}")
                    .when().post("/tasks/" + id + "/subtasks").then().statusCode(201);
        }
        return id;
    }

    @Test
    void aTaskWithoutSubtasksReportsZero() {
        Tenant tenant = data.createTenant();
        String token = tokenFor(tenant);
        String name = "Bare-" + UUID.randomUUID();
        createTask(token, name, 0);

        given().header("Authorization", token)
                .when().get("/tasks")
                .then().statusCode(200)
                .body("items.find { it.name == '" + name + "' }.subtaskCount", equalTo(0));
    }

    @Test
    void aTaskCountsItsOwnSubtasksOnly() {
        Tenant tenant = data.createTenant();
        String token = tokenFor(tenant);
        String counted = "Counted-" + UUID.randomUUID();
        String other = "Other-" + UUID.randomUUID();
        createTask(token, counted, 3);
        createTask(token, other, 1);

        given().header("Authorization", token)
                .when().get("/tasks")
                .then().statusCode(200)
                .body("items.find { it.name == '" + counted + "' }.subtaskCount", equalTo(3))
                .body("items.find { it.name == '" + other + "' }.subtaskCount", equalTo(1));
    }
}
