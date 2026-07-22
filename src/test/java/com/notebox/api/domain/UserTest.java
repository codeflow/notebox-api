package com.notebox.api.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserTest {

    @Inject
    EntityManager em;

    private Tenant persistTenant(String slug) {
        Tenant tenant = new Tenant(UUID.randomUUID(), "Acme " + slug, slug);
        em.persist(tenant);
        return tenant;
    }

    @Test
    @TestTransaction
    void persistsAndReadsBackWithDefaults() {
        Tenant tenant = persistTenant("acme-" + UUID.randomUUID());
        User user = new User(UUID.randomUUID(), tenant.getId(), "a@" + tenant.getSlug() + ".test",
                "hash", null, "Ada");
        em.persist(user);
        em.flush();
        em.clear();

        User found = em.find(User.class, user.getId());
        assertEquals(tenant.getId(), found.getTenantId());
        assertEquals(Role.MEMBER, found.getRole(), "role defaults to MEMBER (INV-4)");
        assertTrue(found.isActive(), "active defaults to true");
        assertNotNull(found.getCreatedAt());
    }

    @Test
    @TestTransaction
    void rejectsDuplicateEmailAcrossTenants() {
        Tenant a = persistTenant("t-a-" + UUID.randomUUID());
        Tenant b = persistTenant("t-b-" + UUID.randomUUID());
        String email = "dup-" + UUID.randomUUID() + "@notebox.test";
        em.persist(new User(UUID.randomUUID(), a.getId(), email, "h", Role.MEMBER, "A"));
        em.flush();

        em.persist(new User(UUID.randomUUID(), b.getId(), email, "h", Role.MEMBER, "B"));
        assertThrows(PersistenceException.class, em::flush, "email is globally unique (INV-5)");
    }
}
