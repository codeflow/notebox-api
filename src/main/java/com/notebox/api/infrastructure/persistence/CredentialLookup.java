package com.notebox.api.infrastructure.persistence;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import com.notebox.api.domain.User;

/**
 * Looks up a user by globally-unique email for authentication. This is the one legitimately
 * un-tenant-scoped query (AD-03 exception): it runs before a tenant context exists, at the auth entry
 * point. All post-authentication access goes through the tenant-scoped repositories.
 */
@ApplicationScoped
public class CredentialLookup {

    private final EntityManager em;

    public CredentialLookup(EntityManager em) {
        this.em = em;
    }

    public Optional<User> findByEmail(String email) {
        return em.createQuery("select u from User u where u.email = :email", User.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }
}
