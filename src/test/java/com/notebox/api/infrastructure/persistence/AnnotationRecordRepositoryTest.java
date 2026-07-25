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
}
