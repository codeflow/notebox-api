package com.notebox.api.infrastructure.persistence;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.domain.AuditLog;

/** Tenant-scoped audit trail (AD-03, C-10). Append-only in practice. */
@ApplicationScoped
public class AuditLogRepository extends TenantScopedRepository<AuditLog> {

    @Override
    protected Class<AuditLog> entityType() {
        return AuditLog.class;
    }

    /** Number of audit entries the caller's tenant holds for a given target id. */
    public long countForTarget(UUID targetId) {
        return em.createQuery(
                        "select count(a) from AuditLog a where a.tenantId = :tenant and a.targetId = :target",
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("target", targetId)
                .getSingleResult();
    }
}
