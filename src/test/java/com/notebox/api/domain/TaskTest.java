package com.notebox.api.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Pure unit tests (no container, no DB) for the derived math on {@link Task}: the BR-06 status
 * ("A non-integer proportion rounds to an integer percent", OQ-22, the empty-task rule, the
 * recompute wiring) and the BR-07 dates (FR-12, OQ-05: start = min subtask start, end = max subtask
 * end — by date, not position; one-sided and dateless subtasks; the legal inverted pair).
 */
class TaskTest {

    @Test
    void percentOf_noSubtasks_isZero() {
        assertEquals(0, Task.percentOf(0, 0));
    }

    @Test
    void percentOf_cleanDivisions_exactPercent() {
        assertEquals(50, Task.percentOf(1, 2));
        assertEquals(50, Task.percentOf(2, 4));
        assertEquals(100, Task.percentOf(3, 3));
        assertEquals(25, Task.percentOf(1, 4));
    }

    @Test
    void percentOf_nonIntegerProportions_roundHalfUp() {
        assertEquals(33, Task.percentOf(1, 3));
        assertEquals(67, Task.percentOf(2, 3));
        assertEquals(13, Task.percentOf(1, 8));
        assertEquals(17, Task.percentOf(1, 6));
    }

    @Test
    void constructor_newTask_statusZero() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);

        assertEquals(0, task.getStatus());
    }

    @Test
    void recomputeStatus_oneOfTwoDone_fifty() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("Inventory", null, null, true));
        task.addSubtask(new Subtask("Cutover", null, null, false));

        task.recomputeStatus();

        assertEquals(50, task.getStatus());
    }

    @Test
    void recomputeStatus_lastSubtaskRemoved_backToZero() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.LOW);
        Subtask done = new Subtask("Inventory", null, null, true);
        task.addSubtask(done);
        task.recomputeStatus();
        assertEquals(100, task.getStatus());

        task.getSubtasks().clear();
        task.recomputeStatus();

        assertEquals(0, task.getStatus());
    }

    @Test
    void recomputeStatus_oneOfThreeDone_thirtyThree() {
        Task task = new Task(UUID.randomUUID(), "Ops review", Priority.MEDIUM);
        task.addSubtask(new Subtask("A", null, null, true));
        task.addSubtask(new Subtask("B", null, null, false));
        task.addSubtask(new Subtask("C", null, null, false));

        task.recomputeStatus();

        assertEquals(33, task.getStatus());
    }

    @Test
    void setNameAndPriority_mutation_statusUntouched() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("Inventory", null, null, true));
        task.addSubtask(new Subtask("Cutover", null, null, false));
        task.recomputeStatus();

        task.setName("Migrate RabbitMQ");
        task.setPriority(Priority.CRITICAL);

        assertEquals(50, task.getStatus());
        assertEquals("Migrate RabbitMQ", task.getName());
        assertEquals(Priority.CRITICAL, task.getPriority());
    }

    @Test
    void removeSubtask_unknownId_returnsFalseAndKeepsAll() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("Inventory", null, null, false));

        boolean removed = task.removeSubtask(UUID.randomUUID());

        assertEquals(false, removed);
        assertEquals(1, task.getSubtasks().size());
        assertTrue(task.subtask(UUID.randomUUID()).isEmpty());
    }

    // ---- FR-12 / BR-07: derived dates -------------------------------------------------------

    @Test
    void recomputeDates_minStartMaxEnd_regardlessOfInsertionOrder() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("Cutover", LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 10), false));
        task.addSubtask(new Subtask("Inventory", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), false));

        task.recomputeDates();

        assertEquals(LocalDate.of(2026, 9, 1), task.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 10), task.getEndDate());
    }

    @Test
    void recomputeDates_subtaskMissingOneDate_doesNotParticipateInThatBound() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("A", LocalDate.of(2026, 9, 1), null, false));
        task.addSubtask(new Subtask("B", null, LocalDate.of(2026, 9, 10), false));
        task.addSubtask(new Subtask("C", null, null, false));

        task.recomputeDates();

        assertEquals(LocalDate.of(2026, 9, 1), task.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 10), task.getEndDate());
    }

    @Test
    void recomputeDates_noDatedSubtasks_noDates() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("A", null, null, false));

        task.recomputeDates();

        assertNull(task.getStartDate());
        assertNull(task.getEndDate());
    }

    @Test
    void recomputeDates_noSubtasks_noDates() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);

        task.recomputeDates();

        assertNull(task.getStartDate());
        assertNull(task.getEndDate());
    }

    @Test
    void recomputeDates_singleSubtask_itsOwnDates() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("A", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), false));

        task.recomputeDates();

        assertEquals(LocalDate.of(2026, 9, 1), task.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 5), task.getEndDate());
    }

    @Test
    void recomputeDates_disjointOneSidedSubtasks_invertedPairIsReportedAsComputed() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("A", LocalDate.of(2026, 9, 10), null, false));
        task.addSubtask(new Subtask("B", null, LocalDate.of(2026, 9, 1), false));

        task.recomputeDates();

        assertEquals(LocalDate.of(2026, 9, 10), task.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 1), task.getEndDate());
    }

    @Test
    void recomputeDates_afterRemovingBoundarySubtask_boundMoves() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        Subtask early = new Subtask("A", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), false);
        task.addSubtask(early);
        task.addSubtask(new Subtask("B", LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 10), false));
        task.recomputeDates();
        assertEquals(LocalDate.of(2026, 9, 1), task.getStartDate());

        task.getSubtasks().remove(early);
        task.recomputeDates();

        assertEquals(LocalDate.of(2026, 9, 5), task.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 10), task.getEndDate());
    }

    @Test
    void recomputeDates_leavesStatusUntouched() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("A", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), true));
        task.addSubtask(new Subtask("B", null, null, false));
        task.recomputeStatus();

        task.recomputeDates();

        assertEquals(50, task.getStatus());
    }

    // ---- FR-13 / OQ-06: card value object ---------------------------------------------------

    @Test
    void card_absentByDefault_onTaskAndSubtask() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        Subtask subtask = new Subtask("A", null, null, false);

        assertNull(task.getCard());
        assertNull(subtask.getCard());
        assertNull(task.getDetails());
    }

    @Test
    void card_setAndCleared_onTaskAndSubtask() {
        Task task = new Task(UUID.randomUUID(), "Migrate broker", Priority.HIGH);
        Subtask subtask = new Subtask("A", null, null, false);

        task.setCard(new Card("PAY-231", "https://tracker.example/PAY-231"));
        subtask.setCard(new Card("LEG-7", null));

        assertEquals("PAY-231", task.getCard().getCode());
        assertEquals("https://tracker.example/PAY-231", task.getCard().getUrl());
        assertEquals("LEG-7", subtask.getCard().getCode());
        assertNull(subtask.getCard().getUrl());

        task.setCard(null);
        subtask.setCard(null);

        assertNull(task.getCard());
        assertNull(subtask.getCard());
    }

    @Test
    void card_equalsByValue() {
        assertEquals(new Card("PAY-231", "https://tracker.example/PAY-231"),
                new Card("PAY-231", "https://tracker.example/PAY-231"));
        assertEquals(new Card("PAY-231", null).hashCode(), new Card("PAY-231", null).hashCode());
    }
}
