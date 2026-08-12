package com.notebox.api.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.domain.AnnotationRecord;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.AnnotationValue;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.TypeField;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** Persistence of the annotation-record aggregate through the tenant-scoped choke point (AD-03, BR-03). */
@QuarkusTest
class AnnotationRecordRepositoryTest {

    @Inject
    AnnotationRecordRepository repository;

    @Inject
    AnnotationTypeRepository typeRepository;

    @Inject
    TestData data;

    @Inject
    EntityManager em;

    @InjectMock
    TenantContext tenantContext;

    private AnnotationType typeWithPortField(Tenant tenant) {
        AnnotationType type = new AnnotationType(tenant.getId(), "RabbitMQ");
        type.addField(new TypeField("Port", FieldType.NUMBER));
        typeRepository.persistInTenant(type);
        em.flush();
        return type;
    }

    @Test
    @TestTransaction
    void persistsAggregate_withValues() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeWithPortField(tenant);

        AnnotationRecord record = new AnnotationRecord(tenant.getId(), type.getId(), "prod-broker");
        AnnotationValue port = new AnnotationValue(type.getFields().get(0).getId());
        port.setNumberValue(new BigDecimal("5672"));
        record.addValue(port);

        repository.persistInTenant(record);
        em.flush();
        em.clear();

        AnnotationRecord reloaded = repository.findByIdInTenant(record.getId()).orElseThrow();
        assertEquals("prod-broker", reloaded.getName());
        assertEquals(1, reloaded.getValues().size());
        assertEquals(0, new BigDecimal("5672").compareTo(reloaded.getValues().get(0).getNumberValue()),
                "number value round-trips (scale is DECIMAL(38,10))");
    }

    @Test
    @TestTransaction
    void crossTenantReadReturnsEmpty() {
        Tenant tenantA = data.createTenant();
        Tenant tenantB = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenantB.getId());
        AnnotationType typeB = typeWithPortField(tenantB);
        AnnotationRecord recordB = new AnnotationRecord(tenantB.getId(), typeB.getId(), "OwnedByB");
        repository.persistInTenant(recordB);
        em.flush();
        em.clear();

        when(tenantContext.tenantId()).thenReturn(tenantA.getId());
        assertFalse(repository.findByIdInTenant(recordB.getId()).isPresent(),
                "tenant A cannot read tenant B's annotation record (BR-01)");
    }

    @Test
    @TestTransaction
    void existsByType_reflectsWhetherTheTenantHasRecordsOfThatType() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeWithPortField(tenant);

        assertFalse(repository.existsByType(type.getId()), "no records yet");

        repository.persistInTenant(new AnnotationRecord(tenant.getId(), type.getId(), "prod-broker"));
        em.flush();

        assertTrue(repository.existsByType(type.getId()), "a record of this type now exists (OQ-14)");
    }

    // --- feat-008: page + count queries (FR-05, NFR-08, OQ-20) ---

    @Test
    @TestTransaction
    void listByTypeInTenant_ordersNewestFirstWithIdTiebreak() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeWithPortField(tenant);
        // Created in one transaction: timestamps can collide, so the id tiebreak must decide (OQ-20).
        for (int i = 0; i < 5; i++) {
            repository.persistInTenant(new AnnotationRecord(tenant.getId(), type.getId(), "r" + i));
            em.flush();
        }
        em.clear();

        java.util.List<AnnotationRecord> page = repository.listByTypeInTenant(type.getId(), 0, 50);

        assertEquals(5, page.size());
        for (int i = 0; i < page.size() - 1; i++) {
            AnnotationRecord a = page.get(i);
            AnnotationRecord b = page.get(i + 1);
            boolean ordered = a.getCreatedAt().isAfter(b.getCreatedAt())
                    || (a.getCreatedAt().equals(b.getCreatedAt())
                            && a.getId().toString().compareTo(b.getId().toString()) > 0);
            assertTrue(ordered, "rows are createdAt desc with id desc tiebreak (OQ-20)");
        }
    }

    @Test
    @TestTransaction
    void listByTypeInTenant_pagesAndCounts() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeWithPortField(tenant);
        for (int i = 0; i < 7; i++) {
            repository.persistInTenant(new AnnotationRecord(tenant.getId(), type.getId(), "r" + i));
        }
        em.flush();
        em.clear();

        assertEquals(7, repository.countByTypeInTenant(type.getId()));
        assertEquals(3, repository.listByTypeInTenant(type.getId(), 0, 3).size());
        assertEquals(3, repository.listByTypeInTenant(type.getId(), 1, 3).size());
        assertEquals(1, repository.listByTypeInTenant(type.getId(), 2, 3).size());
        assertEquals(0, repository.listByTypeInTenant(type.getId(), 3, 3).size(),
                "a page beyond the end is empty, not an error");
    }

    @Test
    @TestTransaction
    void listByTypeInTenant_isTenantAndTypeScoped() {
        Tenant tenantA = data.createTenant();
        Tenant tenantB = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenantB.getId());
        AnnotationType typeB = typeWithPortField(tenantB);
        repository.persistInTenant(new AnnotationRecord(tenantB.getId(), typeB.getId(), "OwnedByB"));
        em.flush();

        when(tenantContext.tenantId()).thenReturn(tenantA.getId());
        AnnotationType typeA1 = typeWithPortField(tenantA);
        AnnotationType typeA2 = new AnnotationType(tenantA.getId(), "Postgres");
        em.persist(typeA2);
        repository.persistInTenant(new AnnotationRecord(tenantA.getId(), typeA1.getId(), "wanted"));
        repository.persistInTenant(new AnnotationRecord(tenantA.getId(), typeA2.getId(), "otherType"));
        em.flush();
        em.clear();

        when(tenantContext.tenantId()).thenReturn(tenantA.getId());
        java.util.List<AnnotationRecord> page = repository.listByTypeInTenant(typeA1.getId(), 0, 50);
        assertEquals(1, page.size(), "only the requested type of the caller's tenant");
        assertEquals("wanted", page.get(0).getName());
        assertEquals(0, repository.listByTypeInTenant(typeB.getId(), 0, 50).size(),
                "a foreign tenant's type lists empty for tenant A (BR-01/C-01)");
    }
}
