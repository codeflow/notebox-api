package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.restassured.http.ContentType;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Wire contract of the task endpoints (FR-10, FR-11, NFR-08): every spec scenario at the HTTP
 * seam — validation envelope and keys (C-09), auth (C-02), tenant isolation as 404 (C-01),
 * pagination and newest-first order (OQ-21), derived status on the wire (BR-06, OQ-22).
 */
@QuarkusTest
class TaskResourceTest {

    @Inject
    TestData data;

    private String newActorAuth() {
        Tenant tenant = data.createTenant();
        User member = data.createUser(
                tenant.getId(), UUID.randomUUID() + "@tasks.test", "pw-wire", Role.MEMBER);
        return "Bearer " + TestTokens.valid(member.getId(), tenant.getId(), "MEMBER");
    }

    private static String taskJson(String name, String priority) {
        return "{\"name\": \"" + name + "\", \"priority\": \"" + priority + "\"}";
    }

    private static String subtaskJson(String name, boolean done) {
        return "{\"name\": \"" + name + "\", \"done\": " + done + "}";
    }

    private String createTask(String auth, String name, String priority) {
        return given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskJson(name, priority))
                .when().post("/tasks")
                .then().statusCode(201)
                .extract().path("id");
    }

    private String addSubtask(String auth, String taskId, String name, boolean done) {
        return given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(subtaskJson(name, done))
                .when().post("/tasks/" + taskId + "/subtasks")
                .then().statusCode(201)
                .extract().path("subtasks.find { it.name == '" + name + "' }.id");
    }

    @Test
    void createTask_minimumShape_createdAtZeroPercent() {
        String auth = newActorAuth();

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskJson("Migrate broker", "HIGH"))
                .when().post("/tasks")
                .then().statusCode(201)
                .body("name", equalTo("Migrate broker"))
                .body("priority", equalTo("HIGH"))
                .body("status", equalTo(0))
                .body("subtasks", hasSize(0));
    }

    @Test
    void createTask_blankName_rejectedWithLocalizedKey() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskJson("", "LOW"))
                .when().post("/tasks")
                .then().statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("violations.code", hasItem("task.name.required"));
    }

    @Test
    void createTask_priorityOutsideClosedSet_rejected() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskJson("Ops review", "Urgent"))
                .when().post("/tasks")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.priority.invalid"));
    }

    @Test
    void updateTask_supplyingStatus_rejectedAndStatusUnchanged() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");
        addSubtask(auth, taskId, "a", false);
        addSubtask(auth, taskId, "b", false);

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"Migrate broker\", \"priority\": \"HIGH\", \"status\": 80}")
                .when().put("/tasks/" + taskId)
                .then().statusCode(400)
                .body("violations.code", hasItem("task.status.not_writable"));

        given().header("Authorization", auth)
                .when().get("/tasks/" + taskId)
                .then().statusCode(200)
                .body("status", equalTo(0));
    }

    @Test
    void updateTask_nameAndPriority_statusUntouched() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");
        addSubtask(auth, taskId, "Inventory", true);
        addSubtask(auth, taskId, "Cutover", false);

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskJson("Migrate RabbitMQ", "CRITICAL"))
                .when().put("/tasks/" + taskId)
                .then().statusCode(200)
                .body("name", equalTo("Migrate RabbitMQ"))
                .body("priority", equalTo("CRITICAL"))
                .body("status", equalTo(50));
    }

    @Test
    void getTask_returnsSubtasksAndDerivedStatus() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");
        addSubtask(auth, taskId, "Inventory", true);
        addSubtask(auth, taskId, "Cutover", false);

        given().header("Authorization", auth)
                .when().get("/tasks/" + taskId)
                .then().statusCode(200)
                .body("status", equalTo(50))
                .body("subtasks", hasSize(2))
                .body("subtasks.name", hasItem("Inventory"))
                .body("subtasks.find { it.name == 'Inventory' }.done", equalTo(true));
    }

    @Test
    void deleteTask_withSubtasks_thenIndistinguishableFromMissing() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");
        addSubtask(auth, taskId, "a", false);
        addSubtask(auth, taskId, "b", false);
        addSubtask(auth, taskId, "c", false);

        given().header("Authorization", auth)
                .when().delete("/tasks/" + taskId)
                .then().statusCode(204);

        given().header("Authorization", auth)
                .when().get("/tasks/" + taskId)
                .then().statusCode(404)
                .body("code", equalTo("task.not_found"));
    }

    @Test
    void addSubtask_toFullyDoneTask_responseCarriesRecomputedFifty() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");
        addSubtask(auth, taskId, "Inventory", true);

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(subtaskJson("Validate backups", false))
                .when().post("/tasks/" + taskId + "/subtasks")
                .then().statusCode(201)
                .body("status", equalTo(50))
                .body("subtasks", hasSize(2));
    }

    @Test
    void updateSubtask_markSecondOfFourDone_fifty() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Ops review", "MEDIUM");
        addSubtask(auth, taskId, "s1", true);
        String s2 = addSubtask(auth, taskId, "s2", false);
        addSubtask(auth, taskId, "s3", false);
        addSubtask(auth, taskId, "s4", false);

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(subtaskJson("s2", true))
                .when().put("/tasks/" + taskId + "/subtasks/" + s2)
                .then().statusCode(200)
                .body("status", equalTo(50));
    }

    @Test
    void updateSubtask_unmarkDone_recomputesDownward() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");
        String a = addSubtask(auth, taskId, "a", true);
        addSubtask(auth, taskId, "b", true);

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(subtaskJson("a", false))
                .when().put("/tasks/" + taskId + "/subtasks/" + a)
                .then().statusCode(200)
                .body("status", equalTo(50));
    }

    @Test
    void removeSubtask_onlyDoneOne_backToZero() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");
        String done = addSubtask(auth, taskId, "Inventory", true);
        addSubtask(auth, taskId, "b", false);
        addSubtask(auth, taskId, "c", false);
        addSubtask(auth, taskId, "d", false);

        given().header("Authorization", auth)
                .when().delete("/tasks/" + taskId + "/subtasks/" + done)
                .then().statusCode(200)
                .body("status", equalTo(0))
                .body("subtasks", hasSize(3))
                .body("subtasks.name", not(hasItem("Inventory")));
    }

    @Test
    void addSubtask_blankName_rejected() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(subtaskJson("", false))
                .when().post("/tasks/" + taskId + "/subtasks")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.subtask.name.required"));
    }

    @Test
    void addSubtask_startAfterEnd_rejected() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"Cutover\", \"startDate\": \"2026-09-10\", \"endDate\": \"2026-09-01\"}")
                .when().post("/tasks/" + taskId + "/subtasks")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.subtask.date.invalid"));
    }

    @Test
    void getTask_oneOfThreeDone_thirtyThreePercent() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Ops review", "MEDIUM");
        addSubtask(auth, taskId, "A", true);
        addSubtask(auth, taskId, "B", false);
        addSubtask(auth, taskId, "C", false);

        given().header("Authorization", auth)
                .when().get("/tasks/" + taskId)
                .then().statusCode(200)
                .body("status", equalTo(33));
    }

    @Test
    void listTasks_defaultPageSizeIsFifty_totalSignalsMore() {
        String auth = newActorAuth();
        for (int i = 0; i < 51; i++) {
            createTask(auth, "t" + i, "LOW");
        }

        given().header("Authorization", auth)
                .when().get("/tasks")
                .then().statusCode(200)
                .body("items", hasSize(50))
                .body("size", equalTo(50))
                .body("total", equalTo(51));
    }

    @Test
    void listTasks_sizeAboveTwoHundred_rejected() {
        given().header("Authorization", newActorAuth())
                .when().get("/tasks?size=201")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.list.size.out_of_bounds"));
    }

    @Test
    void listTasks_newestFirst() {
        String auth = newActorAuth();
        createTask(auth, "T1", "LOW");
        createTask(auth, "T2", "LOW");
        createTask(auth, "T3", "LOW");

        given().header("Authorization", auth)
                .when().get("/tasks")
                .then().statusCode(200)
                .body("items[0].name", equalTo("T3"))
                .body("items[1].name", equalTo("T2"))
                .body("items[2].name", equalTo("T1"));
    }

    @Test
    void listTasks_neverLeaksAcrossTenants() {
        String authA = newActorAuth();
        String authB = newActorAuth();
        createTask(authA, "A1", "LOW");
        createTask(authA, "A2", "LOW");
        createTask(authB, "B1", "LOW");
        createTask(authB, "B2", "LOW");
        createTask(authB, "B3", "LOW");

        given().header("Authorization", authA)
                .when().get("/tasks")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("total", equalTo(2))
                .body("items.name", not(hasItem("B1")));
    }

    @Test
    void anyTaskEndpoint_unauthenticated_rejected() {
        given()
                .when().get("/tasks")
                .then().statusCode(401);
    }

    @Test
    void foreignTenant_readUpdateDeleteAndSubtaskOps_behaveLikeMissing() {
        String authB = newActorAuth();
        String taskB = createTask(authB, "OwnedByB", "LOW");
        String subtaskB = addSubtask(authB, taskB, "sub", false);

        String authA = newActorAuth();
        given().header("Authorization", authA)
                .when().get("/tasks/" + taskB)
                .then().statusCode(404)
                .body("code", equalTo("task.not_found"));
        given().header("Authorization", authA).contentType(ContentType.JSON)
                .body(taskJson("stolen", "HIGH"))
                .when().put("/tasks/" + taskB)
                .then().statusCode(404);
        given().header("Authorization", authA)
                .when().delete("/tasks/" + taskB)
                .then().statusCode(404);
        given().header("Authorization", authA).contentType(ContentType.JSON)
                .body(subtaskJson("sub", true))
                .when().put("/tasks/" + taskB + "/subtasks/" + subtaskB)
                .then().statusCode(404);

        given().header("Authorization", authB)
                .when().get("/tasks/" + taskB)
                .then().statusCode(200)
                .body("name", equalTo("OwnedByB"));
    }

    @Test
    void validationMessage_resolvesInPortugueseLocale() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .header("Accept-Language", "pt")
                .body(taskJson("", "LOW"))
                .when().post("/tasks")
                .then().statusCode(400)
                .body("violations.find { it.code == 'task.name.required' }.message",
                        equalTo("O nome da tarefa é obrigatório."));
    }

    @Test
    void listTasks_pageAndSizeParams_driveTheQuery() {
        String auth = newActorAuth();
        for (int i = 0; i < 5; i++) {
            createTask(auth, "t" + i, "LOW");
        }

        // Newest first over t0..t4 is t4,t3 | t2,t1 | t0 — page 1 of size 2 must be exactly t2,t1
        // (audit finding 1: a hardcoded service.list(0, 50) must fail here).
        given().header("Authorization", auth)
                .when().get("/tasks?page=1&size=2")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("items[0].name", equalTo("t2"))
                .body("items[1].name", equalTo("t1"))
                .body("total", equalTo(5));

        given().header("Authorization", auth)
                .when().get("/tasks?size=3")
                .then().statusCode(200)
                .body("items", hasSize(3))
                .body("items[0].name", equalTo("t4"));
    }

    @Test
    void listTasks_negativePageOrZeroSize_rejected() {
        String auth = newActorAuth();

        given().header("Authorization", auth)
                .when().get("/tasks?page=-1")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.list.size.out_of_bounds"));

        given().header("Authorization", auth)
                .when().get("/tasks?size=0")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.list.size.out_of_bounds"));
    }

    @Test
    void subtaskDates_acceptedAndRoundTripUnswapped() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");

        // Accept path of the date pair (audit finding 2: a validator rejecting every pair,
        // or a start/end transposition in mapping or DTO, must fail here).
        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"Cutover\", \"startDate\": \"2026-09-01\", \"endDate\": \"2026-09-10\"}")
                .when().post("/tasks/" + taskId + "/subtasks")
                .then().statusCode(201);

        given().header("Authorization", auth)
                .when().get("/tasks/" + taskId)
                .then().statusCode(200)
                .body("subtasks[0].startDate", equalTo("2026-09-01"))
                .body("subtasks[0].endDate", equalTo("2026-09-10"));

        // PUT-replace on dates: a single boundary date is legal, the omitted one clears.
        String subtaskId = given().header("Authorization", auth)
                .when().get("/tasks/" + taskId)
                .then().extract().path("subtasks[0].id");
        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"Cutover\", \"startDate\": \"2026-10-01\"}")
                .when().put("/tasks/" + taskId + "/subtasks/" + subtaskId)
                .then().statusCode(200)
                .body("subtasks[0].startDate", equalTo("2026-10-01"))
                .body("subtasks[0].endDate", equalTo(null));
    }

    @Test
    void createTask_duplicateName_allowed() {
        String auth = newActorAuth();
        createTask(auth, "Migrate broker", "HIGH");
        createTask(auth, "Migrate broker", "LOW");

        given().header("Authorization", auth)
                .when().get("/tasks")
                .then().statusCode(200)
                .body("total", equalTo(2));
    }
}
