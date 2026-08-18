package com.notebox.api.testsupport;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import com.notebox.api.domain.event.SubtaskChange;

/**
 * Test-only observer that records every {@link SubtaskChange} fired, so service tests can assert
 * which domain facts a mutation produced (AD-10) — e.g. that a reschedule fires exactly once and a
 * name-only update fires nothing.
 */
@ApplicationScoped
public class SubtaskChangeRecorder {

    private final List<SubtaskChange> seen = new ArrayList<>();

    void onSubtaskChange(@Observes SubtaskChange change) {
        seen.add(change);
    }

    /** Forgets everything recorded so far — call at the start of a test. */
    public void reset() {
        seen.clear();
    }

    /**
     * The facts recorded since the last reset, in firing order.
     *
     * @return an unmodifiable snapshot
     */
    public List<SubtaskChange> seen() {
        return List.copyOf(seen);
    }
}
