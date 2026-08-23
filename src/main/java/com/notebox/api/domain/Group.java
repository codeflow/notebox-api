package com.notebox.api.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A tenant-owned group (FR-08): a name plus the namespace it lives in, and nothing else. Groups are
 * flat — no parent reference exists, so nesting is unrepresentable rather than merely forbidden
 * (OQ-04) — and a leaf aggregate: membership is held on the item side, so a group is never loaded
 * to read or write the records and tasks that reference it.
 *
 * <p>The table is {@code item_group}: {@code GROUP} is a reserved word in MySQL.
 */
@Entity
@Table(name = "item_group")
public class Group implements TenantOwned {

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", length = 10, nullable = false, updatable = false)
    private GroupDomain domain;

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    protected Group() {
    }

    /**
     * Creates a group in a namespace.
     *
     * @param tenantId the owning tenant (BR-02)
     * @param name the group name, unique within its tenant and domain
     * @param domain the namespace, fixed for the group's lifetime (OQ-04)
     */
    public Group(UUID tenantId, String name, GroupDomain domain) {
        this.tenantId = tenantId;
        this.name = name;
        this.domain = domain;
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

    /**
     * Renames the group. The domain is deliberately not settable — see {@link GroupDomain}.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    public GroupDomain getDomain() {
        return domain;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
