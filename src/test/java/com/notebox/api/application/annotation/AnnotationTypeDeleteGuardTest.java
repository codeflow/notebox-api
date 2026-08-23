package com.notebox.api.application.annotation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.api.dto.AnnotationRecordInput;
import com.notebox.api.api.dto.AnnotationTypeInput;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.error.AnnotationTypeHasRecordsException;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** A type cannot be deleted while it still owns records (OQ-14); an empty type still can (feat-003). */
@QuarkusTest
class AnnotationTypeDeleteGuardTest {

    @Inject
    AnnotationTypeService typeService;

    @Inject
    AnnotationRecordService recordService;

    @Inject
    TestData data;

    @Inject
    EntityManager em;

    @InjectMock
    TenantContext tenantContext;

    @Test
    @TestTransaction
    void delete_isRejectedWhileTheTypeOwnsRecords() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        when(tenantContext.userId()).thenReturn(UUID.randomUUID());

        AnnotationType created = typeService.create(new AnnotationTypeInput("RabbitMQ", null, List.of()));
        em.flush();
        recordService.create(new AnnotationRecordInput(created.getId(), "prod-broker", List.of(), /*group*/ null));
        em.flush();

        assertThrows(AnnotationTypeHasRecordsException.class, () -> typeService.delete(created.getId()));
        assertDoesNotThrow(() -> typeService.get(created.getId()), "the type and its records are unchanged");
    }

    @Test
    @TestTransaction
    void delete_stillSucceedsForAnEmptyType() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        when(tenantContext.userId()).thenReturn(UUID.randomUUID());

        AnnotationType created = typeService.create(new AnnotationTypeInput("Empty", null, List.of()));
        em.flush();
        UUID id = created.getId();

        assertDoesNotThrow(() -> typeService.delete(id));
    }
}
