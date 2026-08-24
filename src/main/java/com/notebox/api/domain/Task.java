package com.notebox.api.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A tenant-owned task (FR-10): a name, a priority from the closed set, an optional inline
 * {@link Card} (FR-13), optional sanitized rich-text details (FR-14), and two kinds of derived
 * state a client can never assign — the status, the integer percent (HALF_UP, OQ-22) of subtasks
 * marked done, 0 when it has none (BR-06); and the dates, min subtask start / max subtask end, null
 * when no subtask contributes (BR-07, OQ-05). Only {@link #recomputeStatus()} and
 * {@link #recomputeDates()} write them. The aggregate root — subtasks are persisted and removed
 * only through it (AD-03).
 */
@Entity
@Table(name = "task")
public class Task implements TenantOwned {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10, nullable = false)
    private Priority priority;

    @Column(name = "status", nullable = false)
    private int status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Embedded
    private Card card;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "group_id", length = 36)
    private UUID groupId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "task_id", nullable = false)
    @OrderBy("createdAt asc, id asc")
    private List<Subtask> subtasks = new ArrayList<>();

    protected Task() {
    }

    public Task(UUID tenantId, String name, Priority priority) {
        this.tenantId = tenantId;
        this.name = name;
        this.priority = priority;
        this.status = 0;
    }

    /**
     * The BR-06 math in one pure function: 0 when there are no subtasks, otherwise the integer
     * percent of done over total, rounded half up (OQ-22: 1/3 → 33, 2/3 → 67, 1/8 → 13).
     *
     * @param doneCount subtasks marked done
     * @param totalCount all subtasks of the task
     * @return the derived completion percentage, 0..100
     */
    public static int percentOf(int doneCount, int totalCount) {
        if (totalCount == 0) {
            return 0;
        }
        return (int) Math.round(doneCount * 100.0d / totalCount);
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public Priority getPriority() {
        return priority;
    }

    public int getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Card getCard() {
        return card;
    }

    public String getDetails() {
        return details;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    /**
     * Sets the rich-text details. Callers must pass the already-sanitized dialect value (C-08);
     * the entity stores what it is given.
     *
     * @param details sanitized rich text, or null for none
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Assigns the task to a group, or clears it. Callers must pass a group id already resolved and
     * domain-checked (FR-08): the entity stores what it is given.
     *
     * @param groupId a task-domain group of the same tenant, or null for ungrouped
     */
    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public void addSubtask(Subtask subtask) {
        this.subtasks.add(subtask);
    }

    /**
     * Finds an owned subtask by id.
     *
     * @param subtaskId the subtask id
     * @return the subtask, or empty when this task owns no subtask with that id
     */
    public Optional<Subtask> subtask(UUID subtaskId) {
        return subtasks.stream().filter(s -> Objects.equals(s.getId(), subtaskId)).findFirst();
    }

    /**
     * Removes an owned subtask; orphanRemoval deletes the row.
     *
     * @param subtaskId the subtask id
     * @return true when a subtask was removed
     */
    public boolean removeSubtask(UUID subtaskId) {
        return subtasks.removeIf(s -> Objects.equals(s.getId(), subtaskId));
    }

    /** Recomputes the derived status from the current subtasks (BR-06) — the only status writer. */
    public void recomputeStatus() {
        int doneCount = (int) subtasks.stream().filter(Subtask::isDone).count();
        this.status = percentOf(doneCount, subtasks.size());
    }

    /**
     * Recomputes the derived dates from the current subtasks (BR-07, FR-12, OQ-05) — the only date
     * writer: start = the earliest subtask start, end = the latest subtask end, each over the
     * subtasks that carry that date and null when none does. The two bounds are independent, so
     * disjoint one-sided subtasks may legitimately yield start after end.
     */
    public void recomputeDates() {
        this.startDate = subtasks.stream()
                .map(Subtask::getStartDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        this.endDate = subtasks.stream()
                .map(Subtask::getEndDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
