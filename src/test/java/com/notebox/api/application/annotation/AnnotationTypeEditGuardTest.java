package com.notebox.api.application.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.api.dto.AnnotationRecordDto;
import com.notebox.api.api.dto.AnnotationRecordInput;
import com.notebox.api.api.dto.AnnotationTypeInput;
import com.notebox.api.api.dto.AnnotationValueInput;
import com.notebox.api.api.dto.FieldOptionInput;
import com.notebox.api.api.dto.TypeFieldInput;
import com.notebox.api.domain.AnnotationRecord;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.AnnotationValue;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.error.AnnotationTypeFieldHasRecordsException;
import com.notebox.api.domain.error.AnnotationTypeFieldSecretFlipException;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/**
 * Type PUT vs existing records: name-matched fields (and label-matched options) keep their identity
 * so values survive; destructive edits are conflicts (OQ-17, OQ-18, audit F5/F12).
 */
@QuarkusTest
class AnnotationTypeEditGuardTest {

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

    private TypeFieldInput textField(String name) {
        return new TypeFieldInput(name, "TEXT", null, null, null, null, null, null);
    }

    private TypeFieldInput secretTextField(String name) {
        return new TypeFieldInput(name, "TEXT", null, null, true, null, null, null);
    }

    private TypeFieldInput numberField(String name) {
        return new TypeFieldInput(name, "NUMBER", null, null, null, BigDecimal.ONE, new BigDecimal("65535"), null);
    }

    private TypeFieldInput choiceField(String name, String... labels) {
        List<FieldOptionInput> options = new ArrayList<>();
        for (String label : labels) {
            options.add(new FieldOptionInput(label, null));
        }
        return new TypeFieldInput(name, "SINGLE_CHOICE", null, null, null, null, null, options);
    }

    private AnnotationTypeInput rabbitInput(String typeName, TypeFieldInput... fields) {
        return new AnnotationTypeInput(typeName, null, List.of(fields));
    }

    private UUID fieldId(AnnotationType type, String name) {
        return type.getFields().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getId();
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

    /** Creates the RabbitMQ type plus one record holding a text, a number and a choice value. */
    private UUID setUpPopulatedType(Tenant tenant) {
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeService.create(rabbitInput(
                "RabbitMQ", textField("URL"), numberField("Port"), choiceField("Environment", "dev", "prod")));
        em.flush();
        AnnotationRecord record = recordService.create(new AnnotationRecordInput(
                type.getId(), "prod-broker",
                List.of(
                        new AnnotationValueInput(fieldId(type, "URL"), "amqp://h", null, null, null),
                        new AnnotationValueInput(fieldId(type, "Port"), null, new BigDecimal("5672"), null, null),
                        new AnnotationValueInput(
                                fieldId(type, "Environment"), null, null, null,
                                List.of(optionId(type, "Environment", "prod"))))));
        em.flush();
        em.clear();
        return record.getId();
    }

    @Test
    @TestTransaction
    void replace_identicalDefinition_preservesEveryRecordValue() {
        Tenant tenant = data.createTenant();
        UUID recordId = setUpPopulatedType(tenant);
        UUID typeId = recordService.get(recordId).getAnnotationTypeId();
        List<UUID> fieldIdsBefore = typeService.get(typeId).getFields().stream().map(f -> f.getId()).toList();

        typeService.replace(typeId, rabbitInput(
                "RabbitMQ", textField("URL"), numberField("Port"), choiceField("Environment", "dev", "prod")));
        em.flush();
        em.clear();

        AnnotationType type = typeService.get(typeId);
        assertEquals(fieldIdsBefore, type.getFields().stream().map(f -> f.getId()).toList(),
                "field identity is pinned — re-minted ids would orphan values invisibly (audit R2-03)");
        AnnotationRecord reloaded = recordService.get(recordId);
        assertEquals(3, AnnotationRecordDto.from(reloaded, type).values().size(),
                "all three values stay reachable through the wire read model, not just as raw rows");
        AnnotationValue choice = valueFor(reloaded, fieldId(type, "Environment"));
        assertEquals(Set.of(optionId(type, "Environment", "prod")), choice.getSelectedOptionIds(),
                "the choice selection survives because option identity is label-preserved");
    }

    @Test
    @TestTransaction
    void replace_renamingTheType_preservesRecordValues() {
        Tenant tenant = data.createTenant();
        UUID recordId = setUpPopulatedType(tenant);
        UUID typeId = recordService.get(recordId).getAnnotationTypeId();

        typeService.replace(typeId, rabbitInput(
                "RabbitMQ brokers", textField("URL"), numberField("Port"), choiceField("Environment", "dev", "prod")));
        em.flush();
        em.clear();

        AnnotationType type = typeService.get(typeId);
        assertEquals("RabbitMQ brokers", type.getName());
        assertEquals(3, AnnotationRecordDto.from(recordService.get(recordId), type).values().size(),
                "values stay reachable through the wire read model after a rename (audit R2-03)");
    }

    @Test
    @TestTransaction
    void replace_addingAField_isAllowedOnAPopulatedType() {
        Tenant tenant = data.createTenant();
        UUID recordId = setUpPopulatedType(tenant);
        UUID typeId = recordService.get(recordId).getAnnotationTypeId();

        typeService.replace(typeId, rabbitInput(
                "RabbitMQ", textField("URL"), numberField("Port"), choiceField("Environment", "dev", "prod"),
                textField("VHost")));
        em.flush();
        em.clear();

        AnnotationType type = typeService.get(typeId);
        assertEquals(4, type.getFields().size());
        assertEquals(3, AnnotationRecordDto.from(recordService.get(recordId), type).values().size(),
                "existing records keep their reachable values, with none for the new field (audit R2-03)");
    }

    @Test
    @TestTransaction
    void replace_removingAField_isRejectedWhileRecordsExist() {
        Tenant tenant = data.createTenant();
        UUID recordId = setUpPopulatedType(tenant);
        UUID typeId = recordService.get(recordId).getAnnotationTypeId();

        assertThrows(AnnotationTypeFieldHasRecordsException.class, () -> typeService.replace(
                typeId, rabbitInput("RabbitMQ", textField("URL"), choiceField("Environment", "dev", "prod"))));
    }

    @Test
    @TestTransaction
    void replace_retypingAField_isRejectedWhileRecordsExist() {
        Tenant tenant = data.createTenant();
        UUID recordId = setUpPopulatedType(tenant);
        UUID typeId = recordService.get(recordId).getAnnotationTypeId();

        assertThrows(AnnotationTypeFieldHasRecordsException.class, () -> typeService.replace(
                typeId, rabbitInput(
                        "RabbitMQ", textField("URL"), textField("Port"), choiceField("Environment", "dev", "prod"))));
    }

    @Test
    @TestTransaction
    void replace_removingAField_succeedsWithNoRecords() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeService.create(rabbitInput("Empty", textField("URL"), numberField("Port")));
        em.flush();
        em.clear();

        typeService.replace(type.getId(), rabbitInput("Empty", textField("URL")));
        em.flush();
        em.clear();

        assertEquals(1, typeService.get(type.getId()).getFields().size(), "feat-003 behaviour preserved");
    }

    @Test
    @TestTransaction
    void replace_flaggingAFieldSecret_isRejectedWhileItHoldsValues() {
        Tenant tenant = data.createTenant();
        UUID recordId = setUpPopulatedType(tenant);
        UUID typeId = recordService.get(recordId).getAnnotationTypeId();

        assertThrows(AnnotationTypeFieldSecretFlipException.class, () -> typeService.replace(
                typeId, rabbitInput(
                        "RabbitMQ", secretTextField("URL"), numberField("Port"),
                        choiceField("Environment", "dev", "prod"))));
    }

    @Test
    @TestTransaction
    void replace_unflaggingASecretField_isRejectedWhileItHoldsValues() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeService.create(rabbitInput("Vault", secretTextField("API key")));
        em.flush();
        recordService.create(new AnnotationRecordInput(
                type.getId(), "prod",
                List.of(new AnnotationValueInput(fieldId(type, "API key"), "s3cr3t", null, null, null))));
        em.flush();
        em.clear();

        assertThrows(AnnotationTypeFieldSecretFlipException.class,
                () -> typeService.replace(type.getId(), rabbitInput("Vault", textField("API key"))));
    }

    @Test
    @TestTransaction
    void replace_secretFlipSucceedsOnceTheFieldHoldsNoValues() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeService.create(rabbitInput("Vault", textField("Notes"), numberField("Port")));
        em.flush();
        recordService.create(new AnnotationRecordInput(
                type.getId(), "prod",
                List.of(new AnnotationValueInput(
                        fieldId(type, "Port"), null, new BigDecimal("5672"), null, null))));
        em.flush();
        em.clear();

        typeService.replace(type.getId(), rabbitInput("Vault", secretTextField("Notes"), numberField("Port")));
        em.flush();
        em.clear();

        assertTrue(typeService.get(type.getId()).getFields().stream()
                        .filter(f -> f.getName().equals("Notes"))
                        .findFirst()
                        .orElseThrow()
                        .isSecret(),
                "the flip is allowed while the field holds no values, even with records present");
    }

    private AnnotationValue valueFor(AnnotationRecord record, UUID fieldId) {
        return record.getValues().stream()
                .filter(v -> v.getTypeFieldId().equals(fieldId))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @TestTransaction
    void replace_identicalDefinitionWithDuplicateFieldNames_preservesBothFieldsAndValues() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeService.create(rabbitInput("Dup", textField("Port"), textField("Port")));
        em.flush();
        UUID firstId = type.getFields().get(0).getId();
        UUID secondId = type.getFields().get(1).getId();
        AnnotationRecord record = recordService.create(new AnnotationRecordInput(
                type.getId(), "both",
                List.of(
                        new AnnotationValueInput(firstId, "a", null, null, null),
                        new AnnotationValueInput(secondId, "b", null, null, null))));
        em.flush();
        em.clear();

        typeService.replace(type.getId(), rabbitInput("Dup", textField("Port"), textField("Port")));
        em.flush();
        em.clear();

        AnnotationType reloadedType = typeService.get(type.getId());
        assertEquals(List.of(firstId, secondId),
                reloadedType.getFields().stream().map(f -> f.getId()).toList(),
                "duplicate-named fields keep their identity positionally (audit R2-02)");
        assertEquals(2, recordService.get(record.getId()).getValues().size(),
                "neither duplicate's value is orphaned by an identical resend");
    }

    @Test
    @TestTransaction
    void replace_droppingOneDuplicateNamedField_isRejectedWhileRecordsExist() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeService.create(rabbitInput("Dup", textField("Port"), textField("Port")));
        em.flush();
        recordService.create(new AnnotationRecordInput(
                type.getId(), "one",
                List.of(new AnnotationValueInput(type.getFields().get(1).getId(), "b", null, null, null))));
        em.flush();
        em.clear();

        assertThrows(AnnotationTypeFieldHasRecordsException.class,
                () -> typeService.replace(type.getId(), rabbitInput("Dup", textField("Port"))),
                "the unmatched duplicate counts as a removal and hits the has-records guard");
    }

    /** Creates an `Env [dev, dev]` type whose single record selects the SECOND `dev`. */
    private UUID setUpDuplicateLabelType(Tenant tenant) {
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = typeService.create(rabbitInput("Dup", choiceField("Env", "dev", "dev")));
        em.flush();
        AnnotationRecord record = recordService.create(new AnnotationRecordInput(
                type.getId(), "picks-the-second",
                List.of(new AnnotationValueInput(
                        fieldId(type, "Env"), null, null, null,
                        List.of(type.getFields().get(0).getOptions().get(1).getId())))));
        em.flush();
        em.clear();
        return record.getId();
    }

    private List<UUID> optionIdsOf(AnnotationType type) {
        return type.getFields().get(0).getOptions().stream().map(o -> o.getId()).toList();
    }

    @Test
    @TestTransaction
    void replace_identicalDefinitionWithDuplicateOptionLabels_preservesBothOptionsAndTheSelection() {
        Tenant tenant = data.createTenant();
        UUID recordId = setUpDuplicateLabelType(tenant);
        UUID typeId = recordService.get(recordId).getAnnotationTypeId();
        List<UUID> optionIdsBefore = optionIdsOf(typeService.get(typeId));
        UUID selectedBefore = optionIdsBefore.get(1);
        em.clear();

        typeService.replace(typeId, rabbitInput("Dup", choiceField("Env", "dev", "dev")));
        em.flush();
        em.clear();

        AnnotationType type = typeService.get(typeId);
        assertEquals(optionIdsBefore, optionIdsOf(type),
                "duplicate-labelled options keep their identity positionally (audit R3-01)");
        AnnotationRecordDto dto = AnnotationRecordDto.from(recordService.get(recordId), type);
        assertEquals(List.of(selectedBefore), dto.values().get(0).optionIds(),
                "the selection stays resolvable through the wire read model — no dangling option id");
        assertTrue(type.getFields().get(0).getOptions().stream()
                        .anyMatch(o -> o.getId().equals(selectedBefore)),
                "the selected option still exists on the type, so a client can render its label");
    }

    @Test
    @TestTransaction
    void replace_droppingOneDuplicateOptionLabel_removesExactlyOneOption() {
        Tenant tenant = data.createTenant();
        UUID recordId = setUpDuplicateLabelType(tenant);
        UUID typeId = recordService.get(recordId).getAnnotationTypeId();
        UUID firstOptionId = optionIdsOf(typeService.get(typeId)).get(0);
        em.clear();

        typeService.replace(typeId, rabbitInput("Dup", choiceField("Env", "dev")));
        em.flush();
        em.clear();

        assertEquals(List.of(firstOptionId), optionIdsOf(typeService.get(typeId)),
                "dropping one label removes exactly the unmatched option, FIFO order — the deferred "
                        + "replace semantics of OQ-17, unchanged by R3-01");
    }
}
