package com.notebox.api.testsupport;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.notebox.api.application.auth.PasswordHasher;
import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;

/** Commits tenants/users so HTTP-driven tests can see them (a rolled-back tx would be invisible). */
@ApplicationScoped
public class TestData {

    private final EntityManager em;
    private final PasswordHasher passwordHasher;

    public TestData(EntityManager em, PasswordHasher passwordHasher) {
        this.em = em;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public Tenant createTenant() {
        Tenant tenant = new Tenant(UUID.randomUUID(), "Tenant", "t-" + UUID.randomUUID());
        em.persist(tenant);
        return tenant;
    }

    @Transactional
    public User createUser(UUID tenantId, String email, String rawPassword, Role role) {
        User user = new User(UUID.randomUUID(), tenantId, email, passwordHasher.hash(rawPassword), role, "Name");
        em.persist(user);
        return user;
    }
}
