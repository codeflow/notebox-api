package com.notebox.api.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A tenant-owned binary image (type/field icon) stored as a MySQL BLOB (AD-04, D6). JSON payloads
 * carry only a reference (id + metadata); the bytes are served from the dedicated binary endpoint.
 */
@Entity
@Table(name = "image")
public class Image implements TenantOwned {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "content_type", length = 40, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Lob
    @Column(name = "bytes", nullable = false)
    private byte[] bytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Image() {
    }

    public Image(UUID tenantId, String contentType, byte[] bytes) {
        this.tenantId = tenantId;
        this.contentType = contentType;
        this.bytes = bytes;
        this.sizeBytes = bytes == null ? 0 : bytes.length;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
