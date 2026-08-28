package com.notebox.api.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.notebox.api.domain.User;
import com.notebox.api.infrastructure.security.TenantContext;

/** Tenant-scoped access to users (AD-03). All queries are filtered by the caller's tenant. */
@ApplicationScoped
public class UserRepository extends TenantScopedRepository<User> {

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    TenantContext tenantContext;

    @Override
    protected Class<User> entityType() {
        return User.class;
    }

    /** Every member of the caller's tenant, oldest first, for the administration screen. */
    public List<User> listAllInTenant() {
        return entityManager.createQuery(
                        "select u from User u where u.tenantId = :tenant order by u.createdAt asc", User.class)
                .setParameter("tenant", tenantContext.tenantId())
                .getResultList();
    }

    /**
     * Finds a member of the caller's tenant by email.
     *
     * <p>Email is globally unique, so this is scoped to the tenant on purpose: an administrator
     * must not be able to learn that an address is taken in some OTHER tenant (BR-01).
     */
    public Optional<User> findByEmailInTenant(String email) {
        return entityManager.createQuery(
                        "select u from User u where u.email = :email and u.tenantId = :tenant", User.class)
                .setParameter("email", email)
                .setParameter("tenant", tenantContext.tenantId())
                .getResultStream()
                .findFirst();
    }
}
