package com.notebox.api.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** Verifies the AD-03 choke point: reads are scoped to the caller's tenant; cross-tenant reads return empty. */
@QuarkusTest
class TenantScopedRepositoryTest {

    @Inject
    UserRepository userRepository;

    @Inject
    TestData data;

    @InjectMock
    TenantContext tenantContext;

    @Test
    @TestTransaction
    void readsOwnTenantResource() {
        Tenant tenantA = data.createTenant();
        User user = data.createUser(tenantA.getId(), "own-" + UUID.randomUUID() + "@t.test", "pw", Role.MEMBER);
        when(tenantContext.tenantId()).thenReturn(tenantA.getId());

        var found = userRepository.findByIdInTenant(user.getId());
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    @TestTransaction
    void crossTenantReadReturnsEmpty() {
        Tenant tenantA = data.createTenant();
        Tenant tenantB = data.createTenant();
        User userInB = data.createUser(tenantB.getId(), "b-" + UUID.randomUUID() + "@t.test", "pw", Role.MEMBER);
        when(tenantContext.tenantId()).thenReturn(tenantA.getId());

        assertFalse(userRepository.findByIdInTenant(userInB.getId()).isPresent(),
                "a member of tenant A cannot read a user owned by tenant B (BR-01)");
    }

    @Test
    @TestTransaction
    void listReturnsOnlyOwnTenant() {
        Tenant tenantA = data.createTenant();
        Tenant tenantB = data.createTenant();
        data.createUser(tenantA.getId(), "a1-" + UUID.randomUUID() + "@t.test", "pw", Role.MEMBER);
        data.createUser(tenantB.getId(), "b1-" + UUID.randomUUID() + "@t.test", "pw", Role.MEMBER);
        when(tenantContext.tenantId()).thenReturn(tenantA.getId());

        boolean allBelongToA = userRepository.listInTenant(0, 100).stream()
                .allMatch(u -> u.getTenantId().equals(tenantA.getId()));
        assertTrue(allBelongToA, "listing never includes another tenant's rows (BR-01)");
    }
}
