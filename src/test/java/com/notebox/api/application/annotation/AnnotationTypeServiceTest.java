package com.notebox.api.application.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.api.dto.AnnotationTypeInput;
import com.notebox.api.api.dto.TypeFieldInput;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.error.AnnotationTypeNameTakenException;
import com.notebox.api.domain.error.AnnotationTypeNotFoundException;
import com.notebox.api.domain.error.ImageNotFoundException;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** Create/list/get: per-field-type defaults (D2, Secret) and the stateful exceptions (FR-01/02/03, BR-03). */
@QuarkusTest
class AnnotationTypeServiceTest {

    @Inject
    AnnotationTypeService service;

    @Inject
    TestData data;

    @Inject
    EntityManager em;

    @InjectMock
    TenantContext tenantContext;

    private Tenant tenantIsSet() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        return tenant;
    }

    private TypeFieldInput field(String name, String type, Boolean secret) {
        return new TypeFieldInput(name, type, null, null, secret, null, null, null);
    }

    @Test
    @TestTransaction
    void create_appliesPerFieldTypeVisibilityAndSecretDefaults() {
        tenantIsSet();
        AnnotationTypeInput input = new AnnotationTypeInput("RabbitMQ", null, List.of(
                field("URL", "TEXT", null),
                field("Notes", "FREE_TEXT", true),
                field("Choice", "SINGLE_CHOICE", null)));

        AnnotationType created = service.create(input);
        em.flush();
        em.clear();

        AnnotationType reloaded = service.get(created.getId());
        assertTrue(reloaded.getFields().get(0).isVisibleForViewing(), "TEXT defaults visible");
        assertFalse(reloaded.getFields().get(0).isSecret(), "secret defaults false");
        assertFalse(reloaded.getFields().get(1).isVisibleForViewing(), "FREE_TEXT defaults not visible (D2)");
        assertTrue(reloaded.getFields().get(1).isSecret(), "secret set on FREE_TEXT persists");
        assertFalse(reloaded.getFields().get(2).isVisibleForViewing(), "SINGLE_CHOICE defaults not visible (D2)");
    }

    @Test
    @TestTransaction
    void create_duplicateNameInTenant_throwsNameTaken() {
        tenantIsSet();
        service.create(new AnnotationTypeInput("Dup", null, List.of()));
        em.flush();
        assertThrows(AnnotationTypeNameTakenException.class,
                () -> service.create(new AnnotationTypeInput("Dup", null, List.of())));
    }

    @Test
    @TestTransaction
    void create_sameNameInDifferentTenant_succeeds() {
        Tenant tenantA = data.createTenant();
        Tenant tenantB = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenantA.getId());
        service.create(new AnnotationTypeInput("Shared", null, List.of()));
        em.flush();

        when(tenantContext.tenantId()).thenReturn(tenantB.getId());
        AnnotationType inB = service.create(new AnnotationTypeInput("Shared", null, List.of()));
        assertNotNull(inB.getId());
    }

    @Test
    @TestTransaction
    void create_allSevenFieldTypes_areAccepted() {
        tenantIsSet();
        AnnotationTypeInput input = new AnnotationTypeInput("Everything", null, List.of(
                field("a", "TEXT", null), field("b", "LIST", null), field("c", "NUMBER", null),
                field("d", "FREE_TEXT", null), field("e", "SINGLE_CHOICE", null),
                field("f", "MULTIPLE_CHOICE", null), field("g", "IMAGE", null)));

        AnnotationType created = service.create(input);
        em.flush();
        em.clear();

        assertEquals(7, service.get(created.getId()).getFields().size());
    }

    @Test
    @TestTransaction
    void create_withUnknownImageReference_throwsImageNotFound() {
        tenantIsSet();
        assertThrows(ImageNotFoundException.class,
                () -> service.create(new AnnotationTypeInput("WithIcon", UUID.randomUUID(), List.of())));
    }

    @Test
    @TestTransaction
    void get_missing_throwsNotFound() {
        tenantIsSet();
        assertThrows(AnnotationTypeNotFoundException.class, () -> service.get(UUID.randomUUID()));
    }
}
