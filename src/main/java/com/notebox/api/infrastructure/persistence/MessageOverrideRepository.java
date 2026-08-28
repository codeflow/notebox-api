package com.notebox.api.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.domain.MessageOverride;

/** Tenant-scoped access to message overrides (FR-16, AD-03). */
@ApplicationScoped
public class MessageOverrideRepository extends TenantScopedRepository<MessageOverride> {

    @Override
    protected Class<MessageOverride> entityType() {
        return MessageOverride.class;
    }

    /** One override, if this tenant has reworded that key in that locale. */
    public Optional<MessageOverride> find(String locale, String key) {
        return em.createQuery(
                        "select m from MessageOverride m where m.tenantId = :tenant"
                                + " and m.locale = :locale and m.messageKey = :key",
                        MessageOverride.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("locale", locale)
                .setParameter("key", key)
                .getResultStream()
                .findFirst();
    }

    /** Everything this tenant has reworded in one locale, key order. */
    public List<MessageOverride> listForLocale(String locale) {
        return em.createQuery(
                        "select m from MessageOverride m where m.tenantId = :tenant"
                                + " and m.locale = :locale order by m.messageKey asc",
                        MessageOverride.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("locale", locale)
                .getResultList();
    }

    /** Drops an override so the key falls back to the product's own catalog. */
    public void remove(MessageOverride override) {
        em.remove(override);
    }

    /** Finds one by id within the caller's tenant. */
    public Optional<MessageOverride> byId(UUID id) {
        return findByIdInTenant(id);
    }
}
