package com.notebox.api.application.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.api.dto.AnnotationRecordInput;
import com.notebox.api.api.dto.AnnotationValueInput;
import com.notebox.api.domain.AnnotationRecord;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.TypeField;
import com.notebox.api.domain.error.SecretRevealForbiddenException;
import com.notebox.api.infrastructure.persistence.AnnotationTypeRepository;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** Reveal is ADMIN-gated, decrypts, and is audited (FR-18, BR-10, C-03, C-10, C-12). */
@QuarkusTest
class AnnotationRecordRevealTest {

    @Inject
    AnnotationRecordService service;

    @Inject
    AnnotationTypeRepository typeRepository;

    @Inject
    AuditLogRepository auditLog;

    @Inject
    TestData data;

    @Inject
    EntityManager em;

    @InjectMock
    TenantContext tenantContext;

    private UUID setUpSecretRecord(Tenant tenant, Role role) {
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        when(tenantContext.role()).thenReturn(role);
        AnnotationType type = new AnnotationType(tenant.getId(), "RabbitMQ");
        TypeField apiKey = new TypeField("API key", FieldType.TEXT);
        apiKey.setSecret(true);
        type.addField(apiKey);
        typeRepository.persistInTenant(type);
        em.flush();

        AnnotationRecord created = service.create(new AnnotationRecordInput(
                type.getId(), "prod-broker",
                List.of(new AnnotationValueInput(
                        type.getFields().get(0).getId(), "s3cr3t-token", null, null, null))));
        em.flush();
        UUID recordId = created.getId();
        em.clear();
        return recordId;
    }

    @Test
    @TestTransaction
    void reveal_returnsCleartextToAdminAndWritesOneAuditEntry() {
        Tenant tenant = data.createTenant();
        UUID actor = UUID.randomUUID();
        UUID recordId = setUpSecretRecord(tenant, Role.ADMIN);
        when(tenantContext.userId()).thenReturn(actor);
        AnnotationRecord record = service.get(recordId);
        UUID fieldId = record.getValues().get(0).getTypeFieldId();

        String cleartext = service.reveal(recordId, fieldId);

        assertEquals("s3cr3t-token", cleartext);
        assertEquals(1, auditLog.countForTarget(recordId), "exactly one audit entry for the reveal");
    }

    @Test
    @TestTransaction
    void reveal_isDeniedToAPlainMember() {
        Tenant tenant = data.createTenant();
        UUID recordId = setUpSecretRecord(tenant, Role.ADMIN);
        AnnotationRecord record = service.get(recordId);
        UUID fieldId = record.getValues().get(0).getTypeFieldId();

        when(tenantContext.role()).thenReturn(Role.MEMBER);

        assertThrows(SecretRevealForbiddenException.class, () -> service.reveal(recordId, fieldId));
        assertEquals(0, auditLog.countForTarget(recordId), "no audit entry when the reveal is forbidden");
    }
}
