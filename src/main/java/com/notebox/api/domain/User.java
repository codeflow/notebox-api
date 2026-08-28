package com.notebox.api.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A tenant-owned identity. Every user belongs to exactly one tenant (BR-02); the password hash is
 * Argon2id and is never serialized into an API response (INV-3, C-04).
 */
@Entity
@Table(name = "app_user")
public class User implements TenantOwned {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "display_name", length = 120, nullable = false)
    private String displayName;

    @Column(name = "email", length = 254, nullable = false, unique = true)
    private String email;

    @Column(name = "locale_preference", length = 5)
    private String localePreference;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(UUID id, UUID tenantId, String email, String passwordHash, Role role, String displayName) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.role == null) {
            this.role = Role.MEMBER;
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getLocalePreference() {
        return localePreference;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setLocalePreference(String localePreference) {
        this.localePreference = localePreference;
    }

    /** Replaces the stored hash. Callers pass an already-hashed value — never a raw password. */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
