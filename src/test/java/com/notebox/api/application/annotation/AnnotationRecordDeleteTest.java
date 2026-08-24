package com.notebox.api.application.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.notebox.api.domain.AuditLog;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.TypeField;
import com.notebox.api.domain.error.AnnotationRecordNotFoundException;
import com.notebox.api.infrastructure.persistence.AnnotationTypeRepository;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** Deleting a record is explicit, irreversible, and audited in the same transaction (FR-06, BR-05, C-10). */
@QuarkusTest
class AnnotationRecordDeleteTest {

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

    @Test
    @TestTransaction
    void delete_removesTheRecordAndItsValuesAndWritesOneAuditEntry() {
        Tenant tenant = data.createTenant();
        UUID actor = UUID.randomUUID();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        when(tenantContext.userId()).thenReturn(actor);

        AnnotationType type = new AnnotationType(tenant.getId(), "RabbitMQ");
        type.addField(new TypeField("URL", FieldType.TEXT));
        typeRepository.persistInTenant(type);
        em.flush();

        AnnotationRecord created = service.create(new AnnotationRecordInput(
                type.getId(), "prod-broker",
                List.of(new AnnotationValueInput(
                        type.getFields().get(0).getId(), "amqp://h", null, null, null)), /*group*/ null));
        em.flush();
        UUID id = created.getId();
        em.clear();

        service.delete(id);
        em.flush();
        em.clear();

        assertThrows(AnnotationRecordNotFoundException.class, () -> service.get(id));
        Number orphanRows = (Number) em.createNativeQuery(
                        "select count(*) from annotation_value where annotation_record_id = :id")
                .setParameter("id", id.toString())
                .getSingleResult();
        assertEquals(0, orphanRows.intValue(), "the record's value rows are removed with it (FR-06)");
        assertEquals(1, auditLog.countForTargetAndAction(id, "ANNOTATION_RECORD_DELETED"),
                "exactly one audit entry, labelled as the delete action");
        AuditLog entry = em.createQuery(
                        "select a from AuditLog a where a.targetId = :id and a.action = 'ANNOTATION_RECORD_DELETED'",
                        AuditLog.class)
                .setParameter("id", id)
                .getSingleResult();
        assertEquals(actor, entry.getActorUserId(), "the entry records WHO deleted (audit R2-05)");
        assertNotNull(entry.getAt(), "the entry records WHEN");
    }
}
