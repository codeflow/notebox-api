package com.notebox.api.api.dto;

/**
 * The workspace summary the home screen draws (design 05, OQ-31).
 *
 * <p>Every number is the caller's own tenant. They are counted in the database rather than derived
 * from a listing, so the home screen never has to page through the workspace to describe it.
 */
public record OverviewDto(
        long typesDefined,
        long records,
        long recordsWithImages,
        long secretFields,
        long openTasks,
        long subtasksDone,
        long subtasksTotal,
        long tasksEndingThisWeek) {
}
