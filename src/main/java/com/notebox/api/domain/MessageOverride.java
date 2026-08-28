package com.notebox.api.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A tenant's own wording for one message key in one locale (FR-16).
 *
 * <p>An override REPLACES the bundled default for that tenant only. Absence is meaningful: a key
 * with no override falls through to the product's own catalog, which is why nothing here is ever
 * created empty.
 */
@Entity
@Table(name = "message_override")
public class MessageOverride implements TenantOwned {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, length = 36, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tenant_id", nullable = false, length = 36, updatable = false)
    private UUID tenantId;

    @Column(name = "locale", nullable = false, length = 5)
    private String locale;

    @Column(name = "message_key", nullable = false, length = 120)
    private String messageKey;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MessageOverride() {
    }

    public MessageOverride(UUID id, UUID tenantId, String locale, String messageKey, String value) {
        this.id = id;
        this.tenantId = tenantId;
        this.locale = locale;
        this.messageKey = messageKey;
        this.value = value;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    public String getLocale() {
        return locale;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getValue() {
        return value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Rewords this override. The timestamp moves so an administrator can see what changed when. */
    public void setValue(String value) {
        this.value = value;
        this.updatedAt = Instant.now();
    }
}
