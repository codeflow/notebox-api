package com.notebox.api.application.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.notebox.api.domain.BadgeColour;
import com.notebox.api.domain.FieldOption;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.TypeField;
import com.notebox.api.domain.error.AnnotationRecordFieldUnknownException;
import com.notebox.api.domain.error.AnnotationRecordValueOptionUnknownException;
import com.notebox.api.domain.error.AnnotationRecordValueOutOfBoundsException;
import com.notebox.api.domain.error.AnnotationRecordValueTypeMismatchException;
import com.notebox.api.infrastructure.persistence.AnnotationTypeRepository;
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

    @InjectMock
    TenantContext tenantContext;

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
        assertEquals(0, new BigDecimal("5673").compareTo(reloaded.getValues().get(0).getNumberValue()));
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
