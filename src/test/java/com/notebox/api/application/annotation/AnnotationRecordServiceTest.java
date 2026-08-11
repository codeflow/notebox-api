package com.notebox.api.application.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.api.dto.AnnotationRecordInput;
import com.notebox.api.api.dto.AnnotationValueInput;
import com.notebox.api.domain.AnnotationRecord;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.AnnotationValue;
import com.notebox.api.domain.AuditLog;
import com.notebox.api.domain.BadgeColour;
import com.notebox.api.domain.FieldOption;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.TypeField;
import com.notebox.api.domain.Role;
import com.notebox.api.domain.error.AnnotationRecordFieldUnknownException;
import com.notebox.api.domain.error.AnnotationRecordValueImageNotFoundException;
import com.notebox.api.domain.error.AnnotationRecordValueOptionUnknownException;
import com.notebox.api.domain.error.AnnotationRecordValueOutOfBoundsException;
import com.notebox.api.domain.error.AnnotationRecordValueTooLongException;
import com.notebox.api.domain.error.AnnotationRecordValueTypeMismatchException;
import com.notebox.api.infrastructure.persistence.AnnotationTypeRepository;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** Create/update conformance validation and secret masking-on-read (FR-04, BR-03, FR-18). */
@QuarkusTest
class AnnotationRecordServiceTest {

    @Inject
    AnnotationRecordService service;

    @Inject
    AnnotationTypeRepository typeRepository;

    @Inject
    TestData data;

    @Inject
    EntityManager em;

    @Inject
    AuditLogRepository auditLog;

    @InjectMock
    TenantContext tenantContext;

    @InjectMock
    ImageService imageService;

    @Test
    @TestTransaction
    void create_rejectsANullOptionIdElement() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);

        assertThrows(AnnotationRecordValueOptionUnknownException.class, () -> service.create(
                new AnnotationRecordInput(type.getId(), "x",
                        List.of(new AnnotationValueInput(fieldId(type, "Environment"), null, null, null,
                                java.util.Arrays.asList((UUID) null))))),
                "a null option id is unknown, never an NPE/500 (audit F13/R2-10)");
    }

    @Test
    @TestTransaction
    void create_persistsTheSecretAsCiphertextInTheStoredColumns() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);

        AnnotationRecord record = service.create(new AnnotationRecordInput(
                type.getId(), "prod",
                List.of(new AnnotationValueInput(fieldId(type, "API key"), "s3cr3t-token", null, null, null))));
        em.flush();

        Object[] row = (Object[]) em.createNativeQuery(
                        "select text_value, secret_ciphertext from annotation_value"
                                + " where annotation_record_id = :id")
                .setParameter("id", record.getId().toString())
                .getSingleResult();
        assertNull(row[0], "the cleartext column stays NULL for a secret value (C-12)");
        byte[] stored = (byte[]) row[1];
        assertNotNull(stored, "the ciphertext column holds the stored bytes");
        assertFalse(new String(stored, java.nio.charset.StandardCharsets.UTF_8).contains("s3cr3t-token"),
                "the stored bytes are ciphertext, not the plaintext (C-12, audit R2-11)");
    }

    private AnnotationType rabbitMqType(Tenant tenant) {
        AnnotationType type = new AnnotationType(tenant.getId(), "RabbitMQ");
        type.addField(new TypeField("URL", FieldType.TEXT));
        TypeField port = new TypeField("Port", FieldType.NUMBER);
        port.setNumberBounds(BigDecimal.ONE, new BigDecimal("65535"));
        type.addField(port);
        TypeField env = new TypeField("Environment", FieldType.SINGLE_CHOICE);
        env.addOption(new FieldOption("dev", BadgeColour.GREEN));
        env.addOption(new FieldOption("staging", BadgeColour.YELLOW));
        env.addOption(new FieldOption("prod", BadgeColour.RED));
        type.addField(env);
        TypeField tags = new TypeField("Tags", FieldType.MULTIPLE_CHOICE);
        tags.addOption(new FieldOption("urgent", null));
        tags.addOption(new FieldOption("ops", null));
        tags.addOption(new FieldOption("db", null));
        type.addField(tags);
        TypeField apiKey = new TypeField("API key", FieldType.TEXT);
        apiKey.setSecret(true);
        type.addField(apiKey);
        type.addField(new TypeField("Diagram", FieldType.IMAGE));
        typeRepository.persistInTenant(type);
        em.flush();
        return type;
    }

    private UUID fieldId(AnnotationType type, String name) {
        return type.getFields().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    @Test
    @TestTransaction
    void create_persistsValuesForDefinedFields() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);

        AnnotationRecord record = service.create(new AnnotationRecordInput(
                type.getId(),
                "prod-broker",
                List.of(
                        new AnnotationValueInput(fieldId(type, "URL"), "amqp://h", null, null, null),
                        new AnnotationValueInput(fieldId(type, "Port"), null, new BigDecimal("5672"), null, null),
                        new AnnotationValueInput(
                                fieldId(type, "Environment"), null, null, null,
                                List.of(optionId(type, "Environment", "prod"))))));

        assertEquals("prod-broker", record.getName());
        assertEquals(3, record.getValues().size());
        assertEquals("amqp://h", valueFor(record, fieldId(type, "URL")).getTextValue());
        assertEquals(0, new BigDecimal("5672").compareTo(valueFor(record, fieldId(type, "Port")).getNumberValue()));
        assertEquals(
                Set.of(optionId(type, "Environment", "prod")),
                valueFor(record, fieldId(type, "Environment")).getSelectedOptionIds());
    }

    @Test
    @TestTransaction
    void create_rejectsAValueForAFieldTheTypeDoesNotDefine() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);

        assertThrows(
                AnnotationRecordFieldUnknownException.class,
                () -> service.create(new AnnotationRecordInput(
                        type.getId(), "prod-broker",
                        List.of(new AnnotationValueInput(UUID.randomUUID(), "us-east", null, null, null)))));
    }

    @Test
    @TestTransaction
    void create_rejectsAValueWhoseDataDoesNotMatchTheFieldType() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);

        assertThrows(
                AnnotationRecordValueTypeMismatchException.class,
                () -> service.create(new AnnotationRecordInput(
                        type.getId(), "prod-broker",
                        List.of(new AnnotationValueInput(fieldId(type, "Port"), "not-a-number", null, null, null)))));
    }

    @Test
    @TestTransaction
    void create_rejectsANumberValueOutsideTheFieldBounds() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);

        assertThrows(
                AnnotationRecordValueOutOfBoundsException.class,
                () -> service.create(new AnnotationRecordInput(
                        type.getId(), "prod-broker",
                        List.of(new AnnotationValueInput(
                                fieldId(type, "Port"), null, new BigDecimal("70000"), null, null)))));
    }

    @Test
    @TestTransaction
    void create_choiceValueMustReferenceAPredefinedOption() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);

        assertThrows(
                AnnotationRecordValueOptionUnknownException.class,
                () -> service.create(new AnnotationRecordInput(
                        type.getId(), "prod-broker",
                        List.of(new AnnotationValueInput(
                                fieldId(type, "Environment"), null, null, null, List.of(UUID.randomUUID()))))));
    }

    @Test
    @TestTransaction
    void create_multipleChoiceAcceptsSeveralPredefinedOptions() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);
        UUID ops = optionId(type, "Tags", "ops");
        UUID db = optionId(type, "Tags", "db");

        AnnotationRecord record = service.create(new AnnotationRecordInput(
                type.getId(), "prod-broker",
                List.of(new AnnotationValueInput(fieldId(type, "Tags"), null, null, null, List.of(ops, db)))));

        assertEquals(Set.of(ops, db), record.getValues().get(0).getSelectedOptionIds());
    }

    @Test
    @TestTransaction
    void update_changesARecordsValues() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);
        AnnotationRecord created = service.create(new AnnotationRecordInput(
                type.getId(), "prod-broker",
                List.of(new AnnotationValueInput(fieldId(type, "Port"), null, new BigDecimal("5672"), null, null))));
        em.flush();
        UUID id = created.getId();
        em.clear();

        service.update(id, new AnnotationRecordInput(
                null, "prod-broker",
                List.of(new AnnotationValueInput(fieldId(type, "Port"), null, new BigDecimal("5673"), null, null))));
        em.flush();
        em.clear();

        AnnotationRecord reloaded = service.get(id);
        assertEquals(1, reloaded.getValues().size(), "the replaced value must not leave an orphan row behind");
        assertEquals(0, new BigDecimal("5673").compareTo(reloaded.getValues().get(0).getNumberValue()));
    }

    @Test
    @TestTransaction
    void create_multipleChoiceAcceptsAnEmptySelection() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);

        AnnotationRecord record = service.create(new AnnotationRecordInput(
                type.getId(), "no-tags",
                List.of(new AnnotationValueInput(fieldId(type, "Tags"), null, null, null, List.of()))));

        assertTrue(valueFor(record, fieldId(type, "Tags")).getSelectedOptionIds().isEmpty(),
                "MULTIPLE_CHOICE allows 0..n selections (contract, audit F9)");
    }

    @Test
    @TestTransaction
    void create_rejectsATextValueOverTheColumnBound() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);

        assertThrows(
                AnnotationRecordValueTooLongException.class,
                () -> service.create(new AnnotationRecordInput(
                        type.getId(), "too-long",
                        List.of(new AnnotationValueInput(fieldId(type, "URL"), "x".repeat(65_536), null, null, null)))));
    }

    @Test
    @TestTransaction
    void create_rejectsASecretValueTheCiphertextColumnCannotHold() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);

        assertThrows(
                AnnotationRecordValueTooLongException.class,
                () -> service.create(new AnnotationRecordInput(
                        type.getId(), "too-long",
                        List.of(new AnnotationValueInput(
                                fieldId(type, "API key"), "x".repeat(4_081), null, null, null)))));
    }

    @Test
    @TestTransaction
    void create_persistsAnImageValueThatExistsInTheTenant() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);
        UUID imageId = UUID.randomUUID();
        when(imageService.existsInTenant(imageId)).thenReturn(true);

        AnnotationRecord record = service.create(new AnnotationRecordInput(
                type.getId(), "with-diagram",
                List.of(new AnnotationValueInput(fieldId(type, "Diagram"), null, null, imageId, null))));

        assertEquals(imageId, valueFor(record, fieldId(type, "Diagram")).getImageId());
    }

    @Test
    @TestTransaction
    void create_rejectsAnImageValueUnknownToTheTenant() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);
        UUID imageId = UUID.randomUUID();
        when(imageService.existsInTenant(imageId)).thenReturn(false);

        assertThrows(
                AnnotationRecordValueImageNotFoundException.class,
                () -> service.create(new AnnotationRecordInput(
                        type.getId(), "with-diagram",
                        List.of(new AnnotationValueInput(fieldId(type, "Diagram"), null, null, imageId, null)))));
    }

    @Test
    @TestTransaction
    void update_preservesTheSecretWhenItsFieldIsOmitted() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);
        UUID apiKey = fieldId(type, "API key");
        AnnotationRecord created = service.create(new AnnotationRecordInput(
                type.getId(), "prod-broker",
                List.of(
                        new AnnotationValueInput(fieldId(type, "URL"), "amqp://h", null, null, null),
                        new AnnotationValueInput(apiKey, "s3cr3t-token", null, null, null))));
        em.flush();
        UUID id = created.getId();
        byte[] ciphertextBefore = valueFor(created, apiKey).getSecretCiphertext().clone();
        em.clear();

        service.update(id, new AnnotationRecordInput(
                null, "prod-renamed",
                List.of(new AnnotationValueInput(fieldId(type, "URL"), "amqp://h2", null, null, null))));
        em.flush();
        em.clear();

        AnnotationRecord reloaded = service.get(id);
        assertEquals("prod-renamed", reloaded.getName());
        assertArrayEquals(ciphertextBefore, valueFor(reloaded, apiKey).getSecretCiphertext(),
                "an omitted secret keeps its ciphertext byte-identical (audit F4)");
    }

    @Test
    @TestTransaction
    void update_treatsAnEchoedMaskedSecretAsPreserveNotTypeMismatch() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);
        UUID apiKey = fieldId(type, "API key");
        AnnotationRecord created = service.create(new AnnotationRecordInput(
                type.getId(), "prod-broker",
                List.of(new AnnotationValueInput(apiKey, "s3cr3t-token", null, null, null))));
        em.flush();
        UUID id = created.getId();
        byte[] ciphertextBefore = valueFor(created, apiKey).getSecretCiphertext().clone();
        em.clear();

        service.update(id, new AnnotationRecordInput(
                null, "prod-broker",
                List.of(new AnnotationValueInput(apiKey, null, null, null, null))));
        em.flush();
        em.clear();

        assertArrayEquals(ciphertextBefore, valueFor(service.get(id), apiKey).getSecretCiphertext(),
                "the masked GET echo (text: null) is a no-op preserve (audit F4)");
    }

    @Test
    @TestTransaction
    void update_reEncryptsTheSecretWhenANewValueIsSent() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        when(tenantContext.userId()).thenReturn(UUID.randomUUID());
        when(tenantContext.role()).thenReturn(Role.ADMIN);
        AnnotationType type = rabbitMqType(tenant);
        UUID apiKey = fieldId(type, "API key");
        AnnotationRecord created = service.create(new AnnotationRecordInput(
                type.getId(), "prod-broker",
                List.of(new AnnotationValueInput(apiKey, "s3cr3t-token", null, null, null))));
        em.flush();
        UUID id = created.getId();
        byte[] ciphertextBefore = valueFor(created, apiKey).getSecretCiphertext().clone();
        em.clear();

        service.update(id, new AnnotationRecordInput(
                null, "prod-broker",
                List.of(new AnnotationValueInput(apiKey, "rotated-token", null, null, null))));
        em.flush();
        em.clear();

        assertFalse(java.util.Arrays.equals(
                        ciphertextBefore, valueFor(service.get(id), apiKey).getSecretCiphertext()),
                "a new secret value is re-encrypted, not preserved");
        assertEquals("rotated-token", service.reveal(id, apiKey));
    }

    @Test
    @TestTransaction
    void update_erasesTheSecretOnlyWithClearSecretAndAuditsIt() {
        Tenant tenant = data.createTenant();
        UUID actor = UUID.randomUUID();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        when(tenantContext.userId()).thenReturn(actor);
        AnnotationType type = rabbitMqType(tenant);
        UUID apiKey = fieldId(type, "API key");
        AnnotationRecord created = service.create(new AnnotationRecordInput(
                type.getId(), "prod-broker",
                List.of(new AnnotationValueInput(apiKey, "s3cr3t-token", null, null, null))));
        em.flush();
        UUID id = created.getId();
        em.clear();

        service.update(id, new AnnotationRecordInput(
                null, "prod-broker",
                List.of(new AnnotationValueInput(apiKey, null, null, null, null, true))));
        em.flush();
        em.clear();

        assertTrue(service.get(id).getValues().isEmpty(), "clearSecret erases the stored value");
        assertEquals(1, auditLog.countForTargetAndAction(id, "ANNOTATION_RECORD_SECRET_CLEARED"),
                "the explicit erasure writes its own audit entry (BR-05, BR-10)");
        AuditLog entry = em.createQuery(
                        "select a from AuditLog a where a.targetId = :id and a.action = :action",
                        AuditLog.class)
                .setParameter("id", id)
                .setParameter("action", "ANNOTATION_RECORD_SECRET_CLEARED")
                .getSingleResult();
        assertEquals("fieldId=" + apiKey, entry.getDetail(),
                "the erasure records WHICH value was erased (audit F6)");
        assertEquals(actor, entry.getActorUserId(), "the entry records WHO erased (audit R2-05)");
        assertNotNull(entry.getAt(), "the entry records WHEN");
    }

    private AnnotationValue valueFor(AnnotationRecord record, UUID fieldId) {
        return record.getValues().stream()
                .filter(v -> v.getTypeFieldId().equals(fieldId))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @TestTransaction
    void get_neverDecryptsASecretValue_ordinaryReadStaysMasked() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = rabbitMqType(tenant);
        AnnotationRecord created = service.create(new AnnotationRecordInput(
                type.getId(), "prod-broker",
                List.of(new AnnotationValueInput(
                        fieldId(type, "API key"), "s3cr3t-token", null, null, null))));
        em.flush();
        UUID id = created.getId();
        em.clear();

        AnnotationRecord reloaded = service.get(id);
        AnnotationValue secretValue = reloaded.getValues().get(0);
        assertTrue(secretValue.isSecret());
        assertNull(secretValue.getTextValue(), "a plain read never carries the cleartext");
    }

    private UUID optionId(AnnotationType type, String fieldName, String label) {
        return type.getFields().stream()
                .filter(f -> f.getName().equals(fieldName))
                .findFirst()
                .orElseThrow()
                .getOptions()
                .stream()
                .filter(o -> o.getLabel().equals(label))
                .findFirst()
                .orElseThrow()
                .getId();
    }
}
