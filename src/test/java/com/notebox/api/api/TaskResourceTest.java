package com.notebox.api.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.testsupport.TestData;
import com.notebox.api.testsupport.TestTokens;

import io.restassured.http.ContentType;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Wire contract of the task endpoints (FR-10..FR-14, NFR-08): every spec scenario at the HTTP
 * seam — validation envelope and keys (C-09), auth (C-02), tenant isolation as 404 (C-01),
 * pagination and newest-first order (OQ-21), derived status (BR-06, OQ-22) and dates (BR-07,
 * OQ-05) on the wire, the inline card (FR-13) and rich-text details sanitized both ways (FR-14,
 * C-08) — including the legacy-row read seam and the listing-row exclusions.
 */
@QuarkusTest
class TaskResourceTest {

    private static final String DIALECT_DETAILS =
            "<p><strong>b</strong> <em>i</em> <u>u</u> <s>s</s> <code>c</code></p>"
                    + "<ol><li><p>one</p></li></ol><ul><li><p>two</p></li></ul>"
                    + "<blockquote><p>q</p></blockquote>"
                    + "<pre data-language=\"sql\"><code>SELECT 1</code></pre>"
                    + "<p><span style=\"color: rgb(200, 30, 30);\">warn</span></p>"
                    + "<p><a href=\"https://example.com\" rel=\"noopener noreferrer\">docs</a></p>"
                    + "<img data-image-id=\"abc123\" alt=\"d\">";

    @Inject
    TestData data;

    @Inject
    EntityManager em;

    @Inject
    UserTransaction utx;

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

    // ---- US-4.2 input contract (feat-012 T-03): poison dates, card bounds, C-09 ----------------

    private static String taskWithCardJson(String name, String cardJson) {
        return "{\"name\": \"" + name + "\", \"priority\": \"HIGH\", \"card\": " + cardJson + "}";
    }

    @Test
    void createTask_supplyingStartDate_rejectedAsDerived() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body("{\"name\": \"Migrate broker\", \"priority\": \"HIGH\", \"startDate\": \"2026-09-01\"}")
                .when().post("/tasks")
                .then().statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("violations.code", hasItem("task.dates.not_writable"));
    }

    @Test
    void updateTask_supplyingEndDate_rejectedAsDerived() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"Migrate broker\", \"priority\": \"HIGH\", \"endDate\": \"2026-09-10\"}")
                .when().put("/tasks/" + taskId)
                .then().statusCode(400)
                .body("violations.code", hasItem("task.dates.not_writable"));
    }

    @Test
    void createTask_cardWithBlankCode_rejected() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskWithCardJson("Broker migration",
                        "{\"code\": \"\", \"url\": \"https://tracker.example/x\"}"))
                .when().post("/tasks")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.card.code.required"));
    }

    @Test
    void createTask_cardWithoutCode_rejected() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskWithCardJson("Broker migration", "{\"url\": \"https://tracker.example/x\"}"))
                .when().post("/tasks")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.card.code.required"));
    }

    @Test
    void createTask_cardUrlNotAbsoluteHttp_rejected() {
        String auth = newActorAuth();
        for (String hostile : new String[] {
                "javascript:alert(1)", "data:text/html;base64,x", "/PAY-231", "ftp://files.example/x"}) {
            given().header("Authorization", auth).contentType(ContentType.JSON)
                    .body(taskWithCardJson("Broker migration",
                            "{\"code\": \"PAY-233\", \"url\": \"" + hostile + "\"}"))
                    .when().post("/tasks")
                    .then().statusCode(400)
                    .body("violations.code", hasItem("task.card.url.invalid"));
        }
    }

    @Test
    void createTask_cardCodeAboveSixty_rejected() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskWithCardJson("Broker migration", "{\"code\": \"" + "X".repeat(61) + "\"}"))
                .when().post("/tasks")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.card.code.too_long"));
    }

    @Test
    void createTask_cardUrlAbove2048_rejected() {
        String longUrl = "https://tracker.example/" + "a".repeat(2048);
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskWithCardJson("Broker migration",
                        "{\"code\": \"PAY-233\", \"url\": \"" + longUrl + "\"}"))
                .when().post("/tasks")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.card.url.too_long"));
    }

    @Test
    void addSubtask_cardUnderTheSameContract_blankCodeRejected() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"Contract review\","
                        + " \"card\": {\"code\": \" \", \"url\": \"https://tracker.example/LEG-7\"}}")
                .when().post("/tasks/" + taskId + "/subtasks")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.card.code.required"));
    }

    @Test
    void addSubtask_cardUrlJavascript_rejected() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"Contract review\","
                        + " \"card\": {\"code\": \"LEG-7\", \"url\": \"javascript:alert(1)\"}}")
                .when().post("/tasks/" + taskId + "/subtasks")
                .then().statusCode(400)
                .body("violations.code", hasItem("task.card.url.invalid"));
    }

    @Test
    void createTask_wellFormedCardAndDetails_acceptedAtTheEdge() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body("{\"name\": \"Broker migration\", \"priority\": \"HIGH\","
                        + " \"card\": {\"code\": \"PAY-231\", \"url\": \"https://tracker.example/PAY-231\"},"
                        + " \"details\": \"<p>plan</p>\"}")
                .when().post("/tasks")
                .then().statusCode(201);
    }

    @Test
    void cardValidationMessage_resolvesInPortugueseLocale() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .header("Accept-Language", "pt")
                .body(taskWithCardJson("Broker migration", "{\"code\": \"\"}"))
                .when().post("/tasks")
                .then().statusCode(400)
                .body("violations.find { it.code == 'task.card.code.required' }.message",
                        equalTo("O código do card é obrigatório quando um card é informado."));
    }

    // ---- US-4.2 read side (feat-012 T-05): derived dates, card and details on the wire ---------

    private static String datedSubtaskJson(String name, String start, String end) {
        return "{\"name\": \"" + name + "\", \"startDate\": \"" + start + "\", \"endDate\": \"" + end + "\"}";
    }

    private static String taskWithDetailsJson(String name, String detailsJsonString) {
        return "{\"name\": \"" + name + "\", \"priority\": \"HIGH\", \"details\": " + detailsJsonString + "}";
    }

    private static String jsonString(String raw) {
        return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Test
    void getTask_derivedDates_minStartMaxEndRegardlessOfInsertionOrder() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");
        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(datedSubtaskJson("Cutover", "2026-09-03", "2026-09-10"))
                .when().post("/tasks/" + taskId + "/subtasks").then().statusCode(201);
        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(datedSubtaskJson("Inventory", "2026-09-01", "2026-09-05"))
                .when().post("/tasks/" + taskId + "/subtasks").then().statusCode(201)
                .body("startDate", equalTo("2026-09-01"), "endDate", equalTo("2026-09-10"));

        given().header("Authorization", auth)
                .when().get("/tasks/" + taskId)
                .then().statusCode(200)
                .body("startDate", equalTo("2026-09-01"))
                .body("endDate", equalTo("2026-09-10"))
                .body("subtasks", hasSize(2));
    }

    @Test
    void createTask_noSubtasks_datesAbsent() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskJson("Migrate broker", "HIGH"))
                .when().post("/tasks")
                .then().statusCode(201)
                .body("startDate", nullValue())
                .body("endDate", nullValue())
                .body("card", nullValue())
                .body("details", nullValue());
    }

    @Test
    void updateSubtask_reschedule_responseAlreadyShowsMovedDates() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");
        String subtaskId = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(datedSubtaskJson("A", "2026-09-01", "2026-09-05"))
                .when().post("/tasks/" + taskId + "/subtasks")
                .then().statusCode(201).extract().path("subtasks[0].id");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(datedSubtaskJson("A", "2026-09-02", "2026-09-08"))
                .when().put("/tasks/" + taskId + "/subtasks/" + subtaskId)
                .then().statusCode(200)
                .body("startDate", equalTo("2026-09-02"))
                .body("endDate", equalTo("2026-09-08"));
    }

    @Test
    void listTasks_rowsCarryDerivedDatesAndCard_neverDetails() {
        String auth = newActorAuth();
        String taskId = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"Broker migration\", \"priority\": \"HIGH\","
                        + " \"card\": {\"code\": \"PAY-231\", \"url\": \"https://tracker.example/PAY-231\"},"
                        + " \"details\": " + jsonString(DIALECT_DETAILS) + "}")
                .when().post("/tasks")
                .then().statusCode(201).extract().path("id");
        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(datedSubtaskJson("A", "2026-09-01", "2026-09-10"))
                .when().post("/tasks/" + taskId + "/subtasks").then().statusCode(201);

        given().header("Authorization", auth)
                .when().get("/tasks")
                .then().statusCode(200)
                .body("items[0].id", equalTo(taskId))
                .body("items[0].startDate", equalTo("2026-09-01"))
                .body("items[0].endDate", equalTo("2026-09-10"))
                .body("items[0].card.code", equalTo("PAY-231"))
                .body("items[0].card.url", equalTo("https://tracker.example/PAY-231"))
                .body("items[0].status", equalTo(0))
                .body("items[0]", not(hasKey("details")));
    }

    @Test
    void createTask_withCard_roundTripsOnReadAndCodeOnlyHasNullUrl() {
        String auth = newActorAuth();
        String withUrl = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskWithCardJson("Broker migration",
                        "{\"code\": \"PAY-231\", \"url\": \"https://tracker.example/PAY-231\"}"))
                .when().post("/tasks")
                .then().statusCode(201)
                .body("card.code", equalTo("PAY-231"))
                .body("card.url", equalTo("https://tracker.example/PAY-231"))
                .extract().path("id");
        given().header("Authorization", auth)
                .when().get("/tasks/" + withUrl)
                .then().statusCode(200)
                .body("card.code", equalTo("PAY-231"))
                .body("card.url", equalTo("https://tracker.example/PAY-231"));

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskWithCardJson("Broker migration 2", "{\"code\": \"PAY-232\"}"))
                .when().post("/tasks")
                .then().statusCode(201)
                .body("card.code", equalTo("PAY-232"))
                .body("card.url", nullValue());
    }

    @Test
    void addSubtask_withCard_returnedTaskShowsSubtaskCard() {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body("{\"name\": \"Contract review\","
                        + " \"card\": {\"code\": \"LEG-7\", \"url\": \"https://tracker.example/LEG-7\"}}")
                .when().post("/tasks/" + taskId + "/subtasks")
                .then().statusCode(201)
                .body("subtasks[0].card.code", equalTo("LEG-7"))
                .body("subtasks[0].card.url", equalTo("https://tracker.example/LEG-7"))
                .body("card", nullValue());
    }

    @Test
    void updateTask_omittingCard_readReturnsNoCard() {
        String auth = newActorAuth();
        String taskId = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskWithCardJson("Broker migration", "{\"code\": \"PAY-231\"}"))
                .when().post("/tasks")
                .then().statusCode(201).extract().path("id");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskJson("Broker migration", "HIGH"))
                .when().put("/tasks/" + taskId)
                .then().statusCode(200)
                .body("card", nullValue());
        given().header("Authorization", auth)
                .when().get("/tasks/" + taskId)
                .then().statusCode(200)
                .body("card", nullValue());
    }

    @Test
    void createTask_dialectCleanDetails_roundTripByteIdentical() {
        String auth = newActorAuth();
        String taskId = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskWithDetailsJson("Broker migration", jsonString(DIALECT_DETAILS)))
                .when().post("/tasks")
                .then().statusCode(201)
                .body("details", equalTo(DIALECT_DETAILS))
                .extract().path("id");

        given().header("Authorization", auth)
                .when().get("/tasks/" + taskId)
                .then().statusCode(200)
                .body("details", equalTo(DIALECT_DETAILS));
    }

    @Test
    void createTask_hostileDetails_strippedOnTheWire() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskWithDetailsJson("Broker migration",
                        jsonString("<p>before</p><script>steal()</script><p onclick=\"x()\">after</p>")))
                .when().post("/tasks")
                .then().statusCode(201)
                .body("details", containsString("before"))
                .body("details", containsString("after"))
                .body("details", not(containsString("<script")))
                .body("details", not(containsString("onclick")));
    }

    @Test
    void createTask_javascriptHrefInDetails_losesHrefKeepsText() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskWithDetailsJson("Broker migration",
                        jsonString("<p><a href=\"javascript:alert(1)\">click</a></p>")))
                .when().post("/tasks")
                .then().statusCode(201)
                .body("details", containsString("click"))
                .body("details", not(containsString("javascript:")));
    }

    @Test
    void createTask_embeddedImage_isAReferenceNeverAFetchedBinary() {
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskWithDetailsJson("Broker migration",
                        jsonString("<img src=\"https://evil.example/x.png\" data-image-id=\"i9\" alt=\"diagram\">")))
                .when().post("/tasks")
                .then().statusCode(201)
                .body("details", containsString("data-image-id=\"i9\""))
                .body("details", containsString("alt=\"diagram\""))
                .body("details", not(containsString("src=")))
                .body("details", not(containsString("evil.example")));
    }

    @Test
    void legacyHostileDetailsRow_isSanitizedOnTheWayOut() throws Exception {
        String auth = newActorAuth();
        String taskId = createTask(auth, "Migrate broker", "HIGH");
        // A row written before this feature's write path: hostile bytes planted straight into the store.
        String hostile = "<p>x</p><script>steal()</script><img data-image-id=\"i1\" onerror=\"steal()\">";
        utx.begin();
        em.createNativeQuery("update task set details = :v where id = :id")
                .setParameter("v", hostile)
                .setParameter("id", taskId)
                .executeUpdate();
        utx.commit();

        given().header("Authorization", auth)
                .when().get("/tasks/" + taskId)
                .then().statusCode(200)
                .body("details", containsString("<p>x</p>"))
                .body("details", containsString("data-image-id=\"i1\""))
                .body("details", not(containsString("<script")))
                .body("details", not(containsString("onerror")));
    }

    @Test
    void updateTask_omittingDetails_clearsThem() {
        String auth = newActorAuth();
        String taskId = given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskWithDetailsJson("Broker migration", jsonString("<p>plan</p>")))
                .when().post("/tasks")
                .then().statusCode(201)
                .body("details", equalTo("<p>plan</p>"))
                .extract().path("id");

        given().header("Authorization", auth).contentType(ContentType.JSON)
                .body(taskJson("Broker migration", "HIGH"))
                .when().put("/tasks/" + taskId)
                .then().statusCode(200)
                .body("details", nullValue());
    }

    @Test
    void createTask_oversizeDetails_rejectedWithLocalizedKey() {
        String oversize = "<p>" + "a".repeat(65_600) + "</p>";
        given().header("Authorization", newActorAuth()).contentType(ContentType.JSON)
                .body(taskWithDetailsJson("Broker migration", jsonString(oversize)))
                .when().post("/tasks")
                .then().statusCode(400)
                .body("code", equalTo("task.details.too_long"));
    }

    @Test
    void foreignTenant_taskWithCardAndDetails_stillBehavesLikeMissing() {
        String authB = newActorAuth();
        String taskId = given().header("Authorization", authB).contentType(ContentType.JSON)
                .body("{\"name\": \"OwnedByB\", \"priority\": \"LOW\","
                        + " \"card\": {\"code\": \"PAY-9\"}, \"details\": \"<p>secret plan</p>\"}")
                .when().post("/tasks")
                .then().statusCode(201).extract().path("id");
        String authA = newActorAuth();

        given().header("Authorization", authA)
                .when().get("/tasks/" + taskId)
                .then().statusCode(404);
        given().header("Authorization", authA).contentType(ContentType.JSON)
                .body(taskJson("Hijack", "HIGH"))
                .when().put("/tasks/" + taskId)
                .then().statusCode(404);
        given().header("Authorization", authA)
                .when().delete("/tasks/" + taskId)
                .then().statusCode(404);
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
