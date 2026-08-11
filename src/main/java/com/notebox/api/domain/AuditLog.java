package com.notebox.api.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A tenant-owned record of an irreversible or administrative action — who did what, to which target,
 * and when (C-10). Written in the same transaction as the action it records (BR-05).
 */
@Entity
@Table(name = "audit_log")
public class AuditLog implements TenantOwned {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private UUID tenantId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "actor_user_id", length = 36, nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "action", length = 60, nullable = false, updatable = false)
    private String action;

    @Column(name = "target_type", length = 60, nullable = false, updatable = false)
    private String targetType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "target_id", length = 36, nullable = false, updatable = false)
    private UUID targetId;

    @Column(name = "at", nullable = false, updatable = false)
    private Instant at;

    @Column(name = "detail", length = 255, updatable = false)
    private String detail;

    protected AuditLog() {
    }

    public AuditLog(UUID tenantId, UUID actorUserId, String action, String targetType, UUID targetId) {
        this(tenantId, actorUserId, action, targetType, targetId, null);
    }

    public AuditLog(
            UUID tenantId, UUID actorUserId, String action, String targetType, UUID targetId, String detail) {
        this.tenantId = tenantId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.at == null) {
            this.at = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public Instant getAt() {
        return at;
    }

    public String getDetail() {
        return detail;
    }
}
