package com.notebox.api.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.BadgeColour;
import com.notebox.api.domain.FieldOption;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.TypeField;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** Persistence of the annotation-type aggregate through the tenant-scoped choke point (AD-03, BR-03). */
@QuarkusTest
class AnnotationTypeRepositoryTest {

    @Inject
    AnnotationTypeRepository repository;

    @Inject
    TestData data;

    @Inject
    EntityManager em;

    @InjectMock
    TenantContext tenantContext;

    @Test
    @TestTransaction
    void persistsAggregate_withOrderedFieldsOptionsAndSecret() {
        Tenant tenantA = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenantA.getId());

        AnnotationType type = new AnnotationType(tenantA.getId(), "RabbitMQ");
        TypeField url = new TypeField("URL", FieldType.TEXT);
        url.setSecret(true);
        TypeField status = new TypeField("Status", FieldType.LIST);
        status.addOption(new FieldOption("open", BadgeColour.GREEN));
        status.addOption(new FieldOption("blocked", BadgeColour.RED));
        type.addField(url);
        type.addField(status);

        repository.persistInTenant(type);
        em.flush();
        em.clear();

        AnnotationType reloaded = repository.findByIdInTenant(type.getId()).orElseThrow();
        assertEquals(2, reloaded.getFields().size());
        assertEquals("URL", reloaded.getFields().get(0).getName(), "declared field order is preserved");
        assertTrue(reloaded.getFields().get(0).isSecret(), "the Secret flag persists");
        assertEquals("Status", reloaded.getFields().get(1).getName());
        assertEquals(2, reloaded.getFields().get(1).getOptions().size());
        assertEquals("open", reloaded.getFields().get(1).getOptions().get(0).getLabel(), "option order is preserved");
        assertEquals(BadgeColour.GREEN, reloaded.getFields().get(1).getOptions().get(0).getBadgeColour());
    }

    @Test
    @TestTransaction
    void crossTenantReadReturnsEmpty() {
        Tenant tenantA = data.createTenant();
        Tenant tenantB = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenantB.getId());
        AnnotationType ownedByB = new AnnotationType(tenantB.getId(), "OwnedByB");
        repository.persistInTenant(ownedByB);
        em.flush();
        em.clear();

        when(tenantContext.tenantId()).thenReturn(tenantA.getId());
        assertFalse(repository.findByIdInTenant(ownedByB.getId()).isPresent(),
                "tenant A cannot read tenant B's annotation type (BR-01)");
    }
}
