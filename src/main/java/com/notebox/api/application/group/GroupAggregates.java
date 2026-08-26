package com.notebox.api.application.group;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.notebox.api.domain.GroupDomain;

/**
 * The per-group aggregates the Navigator and the groups screens display (FR-08, OQ-27). Domain
 * shaped: an annotation group reports {@code typesUsed} and never an average; a task group the
 * reverse. The field that does not apply is null.
 *
 * <p><b>{@code averageStatus} distinguishes null from zero, deliberately.</b> Null means the group
 * holds no tasks; zero means it holds tasks and they are all at 0%. Collapsing the two would report
 * an empty group as a stalled one.
 *
 * @param itemCount records (annotation domain) or tasks (task domain) in the group
 * @param typesUsed distinct annotation types the group's records span, or null on a task group
 * @param averageStatus mean derived status as an integer percent, or null when there are no tasks
 */
public record GroupAggregates(long itemCount, Long typesUsed, Integer averageStatus) {

    /**
     * The aggregates of a group the query returned no row for: it has no members, and therefore no
     * average. {@code typesUsed} is zero rather than null on the annotation side — the field
     * applies to that domain, and the group genuinely spans zero types.
     *
     * @param domain the group's namespace
     * @return zeroed aggregates shaped for that domain
     */
    public static GroupAggregates empty(GroupDomain domain) {
        return domain == GroupDomain.ANNOTATION
                ? new GroupAggregates(0L, 0L, null)
                : new GroupAggregates(0L, null, null);
    }

    /**
     * Aggregates for an annotation group.
     *
     * @param itemCount records in the group
     * @param typesUsed distinct types those records span
     * @return the aggregates
     */
    public static GroupAggregates forAnnotations(long itemCount, long typesUsed) {
        return new GroupAggregates(itemCount, typesUsed, null);
    }

    /**
     * Aggregates for a task group. The average is rounded to an integer percent <b>half up</b> —
     * the rule OQ-22 fixed for the status itself, stated here rather than inherited from whatever
     * the JDK does at {@code .5}.
     *
     * @param itemCount tasks in the group
     * @param averageStatus their mean derived status, or null when the group holds no tasks
     * @return the aggregates
     */
    public static GroupAggregates forTasks(long itemCount, Double averageStatus) {
        Integer rounded = averageStatus == null
                ? null
                : BigDecimal.valueOf(averageStatus).setScale(0, RoundingMode.HALF_UP).intValue();
        return new GroupAggregates(itemCount, null, rounded);
    }
}
