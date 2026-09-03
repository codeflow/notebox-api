package com.notebox.api.application.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.api.dto.CardInput;
import com.notebox.api.api.dto.SubtaskInput;
import com.notebox.api.api.dto.TaskInput;
import com.notebox.api.domain.Card;
import com.notebox.api.domain.Priority;
import com.notebox.api.domain.Role;
import com.notebox.api.domain.Task;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.domain.error.SubtaskNotFoundException;
import com.notebox.api.domain.error.TaskDetailsTooLongException;
import com.notebox.api.domain.error.TaskNotFoundException;
import com.notebox.api.domain.event.SubtaskCompleted;
import com.notebox.api.domain.event.SubtaskRescheduled;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.persistence.TaskRepository;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.SubtaskChangeRecorder;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/**
 * Service-level behavior of FR-10..FR-14: recompute wiring through every subtask mutation kind
 * and both flip directions (BR-06, AD-10), derived dates moving with every add/reschedule/remove and
 * never with a done-flip (BR-07, OQ-05), the inline card on task and subtask with replace-clearing
 * (FR-13, OQ-06), rich-text details sanitized before the byte bound and cleared by omission (FR-14,
 * C-08), audited irreversible deletes (BR-05, C-10), and the foreign-tenant-behaves-like-missing
 * rule (C-01).
 */
@QuarkusTest
class TaskServiceTest {

    private static final LocalDate SEP_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEP_2 = LocalDate.of(2026, 9, 2);
    private static final LocalDate SEP_5 = LocalDate.of(2026, 9, 5);
    private static final LocalDate SEP_8 = LocalDate.of(2026, 9, 8);
    private static final LocalDate SEP_10 = LocalDate.of(2026, 9, 10);

    @Inject
    TaskService service;

    @Inject
    TaskRepository repository;

    @Inject
    AuditLogRepository auditLog;

    @Inject
    TestData data;

    @Inject
    SubtaskChangeRecorder events;

    @Inject
    EntityManager em;

    @InjectMock
    TenantContext tenantContext;

    private Tenant actAsFreshTenant() {
        Tenant tenant = data.createTenant();
        User member = data.createUser(
                tenant.getId(), UUID.randomUUID() + "@tasks.test", "pw-tasks-1", Role.MEMBER);
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        when(tenantContext.userId()).thenReturn(member.getId());
        return tenant;
    }

    private static SubtaskInput subtask(String name, boolean done) {
        return new SubtaskInput(name, null, null, done, null, null);
    }

    private static SubtaskInput subtask(String name, LocalDate start, LocalDate end) {
        return new SubtaskInput(name, start, end, false, null, null);
    }

    private static TaskInput taskWith(String name, CardInput card, String details) {
        return new TaskInput(name, "HIGH", null, null, null, card, details, /*group*/ null);
    }

    private Task reload(UUID taskId) {
        em.flush();
        em.clear();
        return repository.findByIdInTenant(taskId).orElseThrow();
    }

    @Test
    @TestTransaction
    void create_minimumShape_statusZero() {
        actAsFreshTenant();

        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        em.flush();

        assertEquals(0, task.getStatus());
        assertEquals(Priority.HIGH, task.getPriority());
    }

    @Test
    @TestTransaction
    void updateSubtask_markSecondOfFourDone_fifty() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Ops review", "MEDIUM", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("s1", true));
        service.addSubtask(task.getId(), subtask("s2", false));
        service.addSubtask(task.getId(), subtask("s3", false));
        service.addSubtask(task.getId(), subtask("s4", false));
        em.flush();
        assertEquals(25, task.getStatus(), "1 of 4 done before the flip");
        UUID secondId = task.getSubtasks().get(1).getId();

        service.updateSubtask(task.getId(), secondId, subtask("s2", true));
        em.flush();

        assertEquals(50, task.getStatus(), "PRD §4 example: the second done subtask makes 2/4 = 50%");
    }

    @Test
    @TestTransaction
    void addSubtask_toFullyDoneTask_recomputesDownward() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("Inventory", true));
        em.flush();
        assertEquals(100, task.getStatus());

        service.addSubtask(task.getId(), subtask("Validate backups", false));
        em.flush();

        assertEquals(50, task.getStatus());
    }

    @Test
    @TestTransaction
    void updateSubtask_unmarkDone_recomputesDownward() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("a", true));
        service.addSubtask(task.getId(), subtask("b", true));
        em.flush();
        assertEquals(100, task.getStatus());
        UUID firstId = task.getSubtasks().get(0).getId();

        service.updateSubtask(task.getId(), firstId, subtask("a", false));
        em.flush();

        assertEquals(50, task.getStatus());
    }

    @Test
    @TestTransaction
    void removeSubtask_lastDoneOne_zeroAndAudited() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("Inventory", true));
        service.addSubtask(task.getId(), subtask("b", false));
        service.addSubtask(task.getId(), subtask("c", false));
        service.addSubtask(task.getId(), subtask("d", false));
        em.flush();
        assertEquals(25, task.getStatus());
        UUID doneId = task.getSubtasks().get(0).getId();

        service.removeSubtask(task.getId(), doneId);
        em.flush();

        assertEquals(3, task.getSubtasks().size());
        assertEquals(0, task.getStatus());
        assertEquals(1, auditLog.countForTargetAndAction(doneId, "SUBTASK_DELETED"),
                "the subtask deletion is audited (BR-05, C-10)");
    }

    @Test
    @TestTransaction
    void update_nameAndPriority_statusUntouched() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("a", true));
        service.addSubtask(task.getId(), subtask("b", false));
        em.flush();
        assertEquals(50, task.getStatus());

        Task updated = service.update(task.getId(),
                new TaskInput("Migrate RabbitMQ", "CRITICAL", null, null, null, null, null, /*group*/ null));
        em.flush();

        assertEquals("Migrate RabbitMQ", updated.getName());
        assertEquals(Priority.CRITICAL, updated.getPriority());
        assertEquals(50, updated.getStatus(), "an update never recomputes nor assigns status (BR-06)");
    }

    @Test
    @TestTransaction
    void delete_taskWithSubtasks_cascadesAndAudits() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("a", false));
        service.addSubtask(task.getId(), subtask("b", false));
        service.addSubtask(task.getId(), subtask("c", false));
        em.flush();
        UUID taskId = task.getId();

        service.delete(taskId);
        em.flush();
        em.clear();

        assertTrue(repository.findByIdInTenant(taskId).isEmpty(), "the task is gone");
        Number orphanSubtasks = (Number) em.createNativeQuery(
                        "select count(*) from subtask where task_id = ?1")
                .setParameter(1, taskId.toString())
                .getSingleResult();
        assertEquals(0, orphanSubtasks.longValue(), "its subtasks went with it — one explicit act (BR-05)");
        assertEquals(1, auditLog.countForTargetAndAction(taskId, "TASK_DELETED"),
                "the task deletion is audited with who/what/when (C-10)");
    }

    @Test
    @TestTransaction
    void get_foreignTenantTask_behavesLikeMissing() {
        Tenant tenantB = data.createTenant();
        User memberB = data.createUser(
                tenantB.getId(), UUID.randomUUID() + "@tasks.test", "pw-tasks-2", Role.MEMBER);
        when(tenantContext.tenantId()).thenReturn(tenantB.getId());
        when(tenantContext.userId()).thenReturn(memberB.getId());
        Task taskB = service.create(new TaskInput("OwnedByB", "LOW", null, null, null, null, null, /*group*/ null));
        em.flush();

        actAsFreshTenant();
        assertThrows(TaskNotFoundException.class, () -> service.get(taskB.getId()),
                "a foreign tenant's task is indistinguishable from a missing one (C-01)");
    }

    @Test
    @TestTransaction
    void updateSubtask_unknownSubtask_throwsSubtaskNotFound() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        em.flush();

        assertThrows(SubtaskNotFoundException.class,
                () -> service.updateSubtask(task.getId(), UUID.randomUUID(), subtask("x", true)));
    }

    // ---- FR-12 / BR-07: derived dates through every mutation kind ----------------------------

    @Test
    @TestTransaction
    void addSubtask_withDates_derivesTaskDatesRegardlessOfInsertionOrder() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        em.flush();
        assertNull(task.getStartDate(), "no subtasks — no dates");
        assertNull(task.getEndDate());

        service.addSubtask(task.getId(), subtask("Cutover", LocalDate.of(2026, 9, 3), SEP_10));
        service.addSubtask(task.getId(), subtask("Inventory", SEP_1, SEP_5));
        em.flush();

        assertEquals(SEP_1, task.getStartDate(), "min subtask start, not the first inserted (OQ-05)");
        assertEquals(SEP_10, task.getEndDate(), "max subtask end");
    }

    @Test
    @TestTransaction
    void updateSubtask_reschedule_movesDerivedDatesImmediately() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("A", SEP_1, SEP_5));
        em.flush();
        assertEquals(SEP_1, task.getStartDate());
        assertEquals(SEP_5, task.getEndDate());
        UUID subtaskId = task.getSubtasks().get(0).getId();
        events.reset();

        Task returned = service.updateSubtask(task.getId(), subtaskId, subtask("A", SEP_2, SEP_8));
        em.flush();

        assertEquals(SEP_2, returned.getStartDate(), "the response already shows the moved start");
        assertEquals(SEP_8, returned.getEndDate(), "and the moved end");
        assertEquals(1, events.seen().size(), "a reschedule fires exactly one fact");
        assertInstanceOf(SubtaskRescheduled.class, events.seen().get(0));
    }

    @Test
    @TestTransaction
    void updateSubtask_sameDates_firesNoRescheduleFact() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("A", SEP_1, SEP_5));
        em.flush();
        UUID subtaskId = task.getSubtasks().get(0).getId();
        events.reset();

        service.updateSubtask(task.getId(), subtaskId, subtask("A renamed", SEP_1, SEP_5));
        em.flush();

        assertTrue(events.seen().isEmpty(), "a name-only update with the same dates fires nothing");
        assertEquals(SEP_1, task.getStartDate());
        assertEquals(SEP_5, task.getEndDate());
    }

    @Test
    @TestTransaction
    void updateSubtask_doneFlipOnly_datesUnchangedAndOnlyTheFlipFactFires() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("A", SEP_1, SEP_5));
        em.flush();
        UUID subtaskId = task.getSubtasks().get(0).getId();
        events.reset();

        service.updateSubtask(task.getId(), subtaskId, new SubtaskInput("A", SEP_1, SEP_5, true, null, null));
        em.flush();

        assertEquals(100, task.getStatus(), "the flip still drives status (BR-06)");
        assertEquals(SEP_1, task.getStartDate(), "dates are untouched by a done-flip");
        assertEquals(SEP_5, task.getEndDate());
        assertEquals(1, events.seen().size());
        assertInstanceOf(SubtaskCompleted.class, events.seen().get(0));
    }

    // ---- FR-20 (feat-029 T-05): the completion moment through the real aggregate ----

    /**
     * Spec: "Completing a subtask records the moment", with the BR-06 percentage stated as a number
     * rather than as a claim about history — 1 of 2 done is 50%, and the moment is not an input to it.
     */
    @Test
    @TestTransaction
    void updateSubtask_completing_recordsTheMomentAndStatusStillCountsDone() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, null));
        service.addSubtask(task.getId(), subtask("A", SEP_1, SEP_5));
        service.addSubtask(task.getId(), subtask("B", SEP_1, SEP_5));
        em.flush();
        assertEquals(0, task.getStatus(), "neither done yet");
        UUID subtaskId = task.getSubtasks().get(0).getId();

        service.updateSubtask(task.getId(), subtaskId, new SubtaskInput("A", SEP_1, SEP_5, true, null, null));
        em.flush();

        assertNotNull(task.subtask(subtaskId).orElseThrow().getCompletedAt(), "the transition records WHEN");
        assertEquals(50, task.getStatus(), "status counts the flag, never the moment (BR-06)");
    }

    /** Spec: "A subtask created already done records the moment" — the add path, through the service. */
    @Test
    @TestTransaction
    void addSubtask_bornDone_recordsTheMoment() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, null));

        service.addSubtask(task.getId(), new SubtaskInput("A", SEP_1, SEP_5, true, null, null));
        em.flush();

        assertNotNull(task.getSubtasks().get(0).getCompletedAt(), "birth-as-done records it too");
        assertEquals(100, task.getStatus());
    }

    /**
     * Spec: "Rescheduling a completed subtask keeps its moment". The moment records when work finished;
     * moving the plan afterwards must not rewrite history — and this is exactly the case that makes a
     * subtask late after the fact, so it has to be exact.
     */
    @Test
    @TestTransaction
    void updateSubtask_reschedulingACompletedSubtask_keepsItsMoment() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, null));
        service.addSubtask(task.getId(), new SubtaskInput("A", SEP_1, SEP_5, true, null, null));
        em.flush();
        UUID subtaskId = task.getSubtasks().get(0).getId();
        Instant recorded = task.getSubtasks().get(0).getCompletedAt();

        service.updateSubtask(task.getId(), subtaskId, new SubtaskInput("A", SEP_1, SEP_10, true, null, null));
        em.flush();

        assertSame(recorded, task.subtask(subtaskId).orElseThrow().getCompletedAt(),
                "a reschedule does not re-stamp the moment");
        assertEquals(SEP_10, task.getEndDate(), "but the plan did move (BR-07)");
    }

    /** Un-completing through the service erases it, and the percentage follows the flag back down. */
    @Test
    @TestTransaction
    void updateSubtask_uncompleting_erasesTheMomentAndStatusFollows() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, null));
        service.addSubtask(task.getId(), new SubtaskInput("A", SEP_1, SEP_5, true, null, null));
        em.flush();
        UUID subtaskId = task.getSubtasks().get(0).getId();

        service.updateSubtask(task.getId(), subtaskId, new SubtaskInput("A", SEP_1, SEP_5, false, null, null));
        em.flush();

        assertNull(task.subtask(subtaskId).orElseThrow().getCompletedAt());
        assertEquals(0, task.getStatus());
    }

    @Test
    @TestTransaction
    void removeSubtask_boundarySubtask_recomputesTheBound() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("A", SEP_1, SEP_2));
        service.addSubtask(task.getId(), subtask("B", SEP_5, SEP_10));
        em.flush();
        assertEquals(SEP_1, task.getStartDate());
        UUID earlyId = task.getSubtasks().get(0).getId();

        Task returned = service.removeSubtask(task.getId(), earlyId);
        em.flush();

        assertEquals(SEP_5, returned.getStartDate(), "the bound moved to the surviving subtask");
        assertEquals(SEP_10, returned.getEndDate());
    }

    @Test
    @TestTransaction
    void addSubtask_datelessOnly_taskStaysDateless() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));

        service.addSubtask(task.getId(), subtask("A", null, null));
        service.addSubtask(task.getId(), subtask("B", true));
        em.flush();

        assertNull(task.getStartDate(), "no subtask contributes a start — null, not epoch (INV-1)");
        assertNull(task.getEndDate());
    }

    @Test
    @TestTransaction
    void addSubtask_oneSidedDates_derivedBoundsAreIndependent() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("A", SEP_10, null));
        service.addSubtask(task.getId(), subtask("B", null, SEP_1));
        em.flush();

        assertEquals(SEP_10, task.getStartDate(), "start-only subtask sets the start bound");
        assertEquals(SEP_1, task.getEndDate(), "end-only subtask sets the end bound — inverted pair is legal");
    }

    @Test
    @TestTransaction
    void derivedDates_survivePersistenceAndReload() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null, null, null, null, null, /*group*/ null));
        service.addSubtask(task.getId(), subtask("A", SEP_1, SEP_5));
        service.addSubtask(task.getId(), subtask("B", SEP_2, SEP_10));
        em.flush();
        em.clear();

        Task reloaded = repository.findByIdInTenant(task.getId()).orElseThrow();

        assertEquals(SEP_1, reloaded.getStartDate(), "the stored column carries the derived start");
        assertEquals(SEP_10, reloaded.getEndDate(), "the stored column carries the derived end");
    }

    // ---- FR-13 / OQ-06: inline card on task and subtask ------------------------------------------

    @Test
    @TestTransaction
    void create_withCard_roundTripsCodeAndUrl() {
        actAsFreshTenant();
        Task task = service.create(taskWith("Broker migration",
                new CardInput("PAY-231", "https://tracker.example/PAY-231"), null));

        Task reloaded = reload(task.getId());

        assertEquals(new Card("PAY-231", "https://tracker.example/PAY-231"), reloaded.getCard());
    }

    @Test
    @TestTransaction
    void create_withCodeOnlyCard_urlIsNull() {
        actAsFreshTenant();
        Task task = service.create(taskWith("Broker migration", new CardInput("PAY-232", null), null));

        Task reloaded = reload(task.getId());

        assertEquals("PAY-232", reloaded.getCard().getCode());
        assertNull(reloaded.getCard().getUrl());
    }

    @Test
    @TestTransaction
    void addSubtask_withCard_subtaskCarriesIt() {
        actAsFreshTenant();
        Task task = service.create(taskWith("Broker migration", null, null));
        service.addSubtask(task.getId(), new SubtaskInput("Contract review", null, null, false, null,
                new CardInput("LEG-7", "https://tracker.example/LEG-7")));

        Task reloaded = reload(task.getId());

        assertEquals(new Card("LEG-7", "https://tracker.example/LEG-7"),
                reloaded.getSubtasks().get(0).getCard());
        assertNull(reloaded.getCard(), "the task itself has no card");
    }

    @Test
    @TestTransaction
    void update_omittingCard_clearsIt() {
        actAsFreshTenant();
        Task task = service.create(taskWith("Broker migration",
                new CardInput("PAY-231", "https://tracker.example/PAY-231"), null));
        assertEquals("PAY-231", reload(task.getId()).getCard().getCode());

        service.update(task.getId(), taskWith("Broker migration", null, null));

        assertNull(reload(task.getId()).getCard(),
                "replace semantics: an all-null embeddable materializes as no card at all");
    }

    @Test
    @TestTransaction
    void updateSubtask_omittingCard_clearsIt() {
        actAsFreshTenant();
        Task task = service.create(taskWith("Broker migration", null, null));
        service.addSubtask(task.getId(), new SubtaskInput("Contract review", null, null, false, null,
                new CardInput("LEG-7", null)));
        UUID subtaskId = reload(task.getId()).getSubtasks().get(0).getId();

        service.updateSubtask(task.getId(), subtaskId, subtask("Contract review", false));

        assertNull(reload(task.getId()).getSubtasks().get(0).getCard());
    }

    @Test
    @TestTransaction
    void cards_areInlineCopies_neverShared() {
        actAsFreshTenant();
        Task t1 = service.create(taskWith("T1", new CardInput("PAY-231", "https://tracker.example/a"), null));
        Task t2 = service.create(taskWith("T2", new CardInput("PAY-231", "https://tracker.example/a"), null));

        service.update(t1.getId(), taskWith("T1", new CardInput("PAY-231", "https://tracker.example/b"), null));

        assertEquals("https://tracker.example/b", reload(t1.getId()).getCard().getUrl());
        assertEquals(new Card("PAY-231", "https://tracker.example/a"), reload(t2.getId()).getCard(),
                "T2's card is byte-for-byte unchanged (OQ-06: value object, not a shared entity)");
    }

    // ---- FR-14 / C-08: details sanitized on write, then bounded, cleared by omission -----------

    @Test
    @TestTransaction
    void create_hostileDetails_storedValueIsSanitized() {
        actAsFreshTenant();
        Task task = service.create(taskWith("Broker migration", null,
                "<p>before</p><script>steal()</script><p onclick=\"x()\">after</p>"));

        String stored = reload(task.getId()).getDetails();

        assertTrue(stored.contains("before") && stored.contains("after"), "the text survives");
        assertFalse(stored.contains("<script"), "no script element is stored");
        assertFalse(stored.contains("steal()"), "script bodies are dropped, not just unwrapped");
        assertFalse(stored.contains("onclick"), "no on* attribute is stored");
    }

    @Test
    @TestTransaction
    void details_optionalAndClearedByOmission() {
        actAsFreshTenant();
        Task task = service.create(taskWith("Broker migration", null, null));
        assertNull(reload(task.getId()).getDetails(), "created without details — none");

        service.update(task.getId(), taskWith("Broker migration", null, "<p>plan</p>"));
        assertEquals("<p>plan</p>", reload(task.getId()).getDetails());

        service.update(task.getId(), taskWith("Broker migration", null, null));
        assertNull(reload(task.getId()).getDetails(), "an update omitting details clears them");
    }

    @Test
    @TestTransaction
    void create_detailsOverSixtyFourKbAfterSanitization_rejected() {
        actAsFreshTenant();
        String oversize = "<p>" + "a".repeat(65_600) + "</p>";

        assertThrows(TaskDetailsTooLongException.class,
                () -> service.create(taskWith("Broker migration", null, oversize)));
    }

    @Test
    @TestTransaction
    void create_detailsExactlyAtTheByteBound_accepted_oneOver_rejected() {
        actAsFreshTenant();
        // "<p>" + n × 'a' + "</p>" is 7 + n bytes; the dialect keeps it byte-identical.
        String atBound = "<p>" + "a".repeat(65_535 - 7) + "</p>";
        String oneOver = "<p>" + "a".repeat(65_536 - 7) + "</p>";
        assertEquals(65_535, atBound.getBytes(StandardCharsets.UTF_8).length, "precondition");
        assertEquals(65_536, oneOver.getBytes(StandardCharsets.UTF_8).length, "precondition");

        Task task = service.create(taskWith("Broker migration", null, atBound));
        assertEquals(atBound, reload(task.getId()).getDetails(), "65 535 bytes is legal (TEXT capacity)");

        assertThrows(TaskDetailsTooLongException.class,
                () -> service.create(taskWith("Broker migration", null, oneOver)),
                "65 536 bytes is one over the TEXT capacity");
    }

    @Test
    @TestTransaction
    void create_detailsOversizeOnlyBeforeSanitization_acceptedBecauseTheBoundMeasuresTheSanitizedValue() {
        actAsFreshTenant();
        // Raw payload is well over 64 KB, but the script body is dropped by the dialect — the stored
        // value is ~65 KB and fits. The bound is applied AFTER sanitization (feat-005 ordering).
        String raw = "<p>" + "a".repeat(65_000) + "</p><script>" + "x".repeat(5_000) + "</script>";
        assertTrue(raw.getBytes(StandardCharsets.UTF_8).length > 65_535, "precondition: raw is oversize");

        Task task = service.create(taskWith("Broker migration", null, raw));

        String stored = reload(task.getId()).getDetails();
        assertTrue(stored.getBytes(StandardCharsets.UTF_8).length <= 65_535);
        assertFalse(stored.contains("<script"));
        assertTrue(stored.startsWith("<p>aaaa"));
    }

    @Test
    @TestTransaction
    void update_hostileDetails_sanitizedExactlyLikeCreate() {
        actAsFreshTenant();
        Task task = service.create(taskWith("Broker migration", null, "<p>clean</p>"));

        service.update(task.getId(), taskWith("Broker migration", null,
                "<a href=\"javascript:alert(1)\">click</a><img src=\"https://evil.example/x.png\" data-image-id=\"i9\">"));

        String stored = reload(task.getId()).getDetails();
        assertTrue(stored.contains("click"));
        assertFalse(stored.contains("javascript:"));
        assertFalse(stored.contains("evil.example"));
        assertTrue(stored.contains("data-image-id=\"i9\""));
    }
}
