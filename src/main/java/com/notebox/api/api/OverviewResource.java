package com.notebox.api.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.notebox.api.api.dto.OverviewDto;
import com.notebox.api.infrastructure.persistence.OverviewRepository;

import io.quarkus.security.Authenticated;

/**
 * The workspace summary the home screen draws (design 05, OQ-31).
 *
 * <p>Read-only and tenant-scoped throughout — the repository takes the tenant from the token and
 * accepts no filter from the caller, so there is no request shape that could widen these counts.
 */
@Path("/overview")
@Authenticated
public class OverviewResource {

    private final OverviewRepository overview;

    public OverviewResource(OverviewRepository overview) {
        this.overview = overview;
    }

    @GET
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public OverviewDto get() {
        // "This week" is Monday..Sunday of the server's current week. The client does not choose it,
        // so two members of the same workspace always see the same number.
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return new OverviewDto(
                overview.typesDefined(),
                overview.records(),
                overview.recordsWithImages(),
                overview.secretFields(),
                overview.openTasks(),
                overview.subtasksDone(),
                overview.subtasksTotal(),
                overview.tasksEndingBetween(weekStart, weekEnd));
    }
}
