package com.notebox.api.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Pure unit tests (no container, no DB) for the BR-06 derived-status math on {@link Task} —
 * the spec scenario "A non-integer proportion rounds to an integer percent" (OQ-22) plus the
 * empty-task rule and the recompute-from-subtasks wiring.
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
}
