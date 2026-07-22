package com.notebox.api.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.domain.User;

/** Tenant-scoped access to users (AD-03). All queries are filtered by the caller's tenant. */
@ApplicationScoped
public class UserRepository extends TenantScopedRepository<User> {

    @Override
    protected Class<User> entityType() {
        return User.class;
    }
}
