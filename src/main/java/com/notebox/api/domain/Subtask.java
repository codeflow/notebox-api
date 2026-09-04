package com.notebox.api.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An aggregate-internal child of a {@link Task} (FR-11): a name, optional start/end dates, an
 * optional inline {@link Card} (FR-13) and the writable done flag that drives the parent's derived
 * status (BR-06); its dates feed the parent's derived dates (BR-07). Carries no tenant id — it is
 * reached only through its task (AD-03) and has no repository of its own.
 */
@Entity
@Table(name = "subtask")
public class Subtask {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "done", nullable = false)
    private boolean done;

    @Embedded
    private Card card;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Subtask() {
    }

    public Subtask(String name, LocalDate startDate, LocalDate endDate, boolean done) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        // Not `this.done = done`: birth-as-done travels the same transition as any other completion,
        // so exactly one expression in the repository assigns completedAt.
        markDone(done);
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

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isDone() {
        return done;
    }

    public Card getCard() {
        return card;
    }

    /**
     * The moment this subtask was marked done.
     *
     * @return the completion moment, or null when it is not done or was completed before FR-20
     */
    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Marks this subtask done or not done, keeping the completion moment consistent with the flag
     * (FR-20): a false-to-true transition records the moment, a true-to-false transition erases it, and
     * a call that does not change the flag leaves the moment exactly as it was — which is also why a
     * subtask completed before FR-20 existed is never back-filled by a later update.
     *
     * <p>This replaces the plain {@code setDone} setter, and the setter is gone on purpose. It is the
     * only guard this codebase can offer: there is no ArchUnit rule and no build gate that would catch
     * a second writer of the flag, so the guard has to be a compile error.
     *
     * @param done the intended done state
     */
    public void markDone(boolean done) {
        if (done == this.done) {
            return;
        }
        this.done = done;
        this.completedAt = done ? Instant.now() : null;
    }


    public void setCard(Card card) {
        this.card = card;
    }
}
