package com.notebox.api.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Pure unit tests (no container, no DB) for the completion transition on {@link Subtask} (FR-20).
 *
 * <p>Six of the feature's twelve scenarios land here, because the rule is entirely a property of the
 * entity: only a change of the done flag writes the moment.
 *
 * <p><b>Why {@code assertSame} and never {@code assertEquals} for the idempotent cases.</b> There is no
 * clock seam in this codebase, so two moments taken back to back are frequently equal at the stored
 * resolution — an {@code assertEquals} would pass whether or not the implementation re-stamped the
 * field. {@code Instant.now()} allocates a fresh object on every call, so object identity is what
 * actually discriminates "left alone" from "written again".
 *
 * <p><b>Not here:</b> the pre-FR-20 shape — done with NO moment — is unreachable through this API by
 * design, so it cannot be built in a unit test without lying about what was constructed. It is
 * asserted at the persistence seam instead, on a row written straight to the database (T-06).
 */
class SubtaskTest {

    private static final LocalDate SEP_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEP_5 = LocalDate.of(2026, 9, 5);

    /** Spec: "Completing a subtask records the moment". */
    @Test
    void markDone_notDoneToDone_recordsTheMoment() {
        Subtask subtask = new Subtask("Cutover", SEP_1, SEP_5, false);
        assertNull(subtask.getCompletedAt(), "a subtask that was never done has no moment");

        subtask.markDone(true);

        assertTrue(subtask.isDone());
        assertNotNull(subtask.getCompletedAt(), "the transition records WHEN");
    }

    /** Spec: "A subtask created already done records the moment" — the add path, not the update path. */
    @Test
    void constructor_bornDone_recordsTheMoment() {
        Subtask subtask = new Subtask("Cutover", SEP_1, SEP_5, true);

        assertTrue(subtask.isDone());
        assertNotNull(subtask.getCompletedAt(), "birth-as-done travels the same transition");
    }

    /** The other half of the constructor: born not done records nothing. */
    @Test
    void constructor_bornNotDone_recordsNoMoment() {
        Subtask subtask = new Subtask("Cutover", SEP_1, SEP_5, false);

        assertNull(subtask.getCompletedAt());
    }

    /** Spec: "Un-completing a subtask erases the moment". */
    @Test
    void markDone_doneToNotDone_erasesTheMoment() {
        Subtask subtask = new Subtask("Cutover", SEP_1, SEP_5, true);

        subtask.markDone(false);

        assertNull(subtask.getCompletedAt(), "un-completing erases it, and it is stored nowhere else");
    }

    /**
     * Spec: "Completing again records a moment, never a stale one". The erase is what makes staleness
     * impossible, so this asserts presence rather than an ordering between two near-simultaneous
     * instants — an ordering assertion here would discriminate nothing.
     */
    @Test
    void markDone_completedAgainAfterUncompleting_recordsAMoment() {
        Subtask subtask = new Subtask("Cutover", SEP_1, SEP_5, true);
        subtask.markDone(false);
        assertNull(subtask.getCompletedAt());

        subtask.markDone(true);

        assertNotNull(subtask.getCompletedAt());
    }

    /** Spec: "An update that leaves done unchanged leaves the moment unchanged". */
    @Test
    void markDone_alreadyDone_leavesTheMomentUntouched() {
        Subtask subtask = new Subtask("Cutover", SEP_1, SEP_5, true);
        Instant recorded = subtask.getCompletedAt();

        subtask.markDone(true);

        assertSame(recorded, subtask.getCompletedAt(), "no transition, no write — the same object");
    }

    /** Spec: "Completing does not move the planned dates". */
    @Test
    void markDone_completing_leavesThePlannedDatesAlone() {
        Subtask subtask = new Subtask("Cutover", SEP_1, SEP_5, false);

        subtask.markDone(true);

        assertSame(SEP_1, subtask.getStartDate());
        assertSame(SEP_5, subtask.getEndDate());
    }

    /** A subtask that was never done stays without a moment when it is un-marked again. */
    @Test
    void markDone_notDoneToNotDone_staysWithoutAMoment() {
        Subtask subtask = new Subtask("Cutover", SEP_1, SEP_5, false);

        subtask.markDone(false);

        assertNull(subtask.getCompletedAt());
        assertTrue(!subtask.isDone());
    }
}
