package com.notebox.api.application.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.api.dto.AnnotationTypeInput;
import com.notebox.api.api.dto.TypeFieldInput;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.error.AnnotationTypeNotFoundException;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** PUT-replace reorders fields; delete is irreversible and writes one audit entry in the same tx (BR-05, C-10). */
@QuarkusTest
class AnnotationTypeDeleteTest {

    @Inject
    AnnotationTypeService service;

    @Inject
    AuditLogRepository auditLog;

    @Inject
    TestData data;

    @Inject
    EntityManager em;

    @InjectMock
    TenantContext tenantContext;

    private TypeFieldInput text(String name) {
        return new TypeFieldInput(name, "TEXT", null, null, null, null, null, null);
    }

    @Test
    @TestTransaction
    void replace_reordersFields() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType created = service.create(
                new AnnotationTypeInput("Reorder", null, List.of(text("A"), text("B"), text("C"))));
        em.flush();
        UUID id = created.getId();
        em.clear();

        service.replace(id, new AnnotationTypeInput("Reorder", null, List.of(text("C"), text("A"), text("B"))));
        em.flush();
        em.clear();

        AnnotationType reloaded = service.get(id);
        assertEquals("C", reloaded.getFields().get(0).getName());
        assertEquals("A", reloaded.getFields().get(1).getName());
        assertEquals("B", reloaded.getFields().get(2).getName());
    }

    @Test
    @TestTransaction
    void delete_removesTypeAndWritesOneAuditEntry() {
        Tenant tenant = data.createTenant();
        UUID actor = UUID.randomUUID();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        when(tenantContext.userId()).thenReturn(actor);
        AnnotationType created = service.create(
                new AnnotationTypeInput("ToDelete", null, List.of(text("A"))));
        em.flush();
        UUID id = created.getId();
        em.clear();

        service.delete(id);
        em.flush();
        em.clear();

        assertThrows(AnnotationTypeNotFoundException.class, () -> service.get(id));
        assertEquals(1, auditLog.countForTarget(id), "exactly one audit entry for the deleted type");
    }
}
