package com.notebox.api.api.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import com.notebox.api.api.dto.FieldOptionInput;
import com.notebox.api.api.dto.TypeFieldInput;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** The custom Bean Validation constraints reject each malformed field declaration (AD-07, FR-02, FR-03). */
@QuarkusTest
class ConstraintsTest {

    @Inject
    Validator validator;

    private Set<String> messages(TypeFieldInput field) {
        return validator.validate(field).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    private TypeFieldInput field(
            String name, String type, Boolean secret, BigDecimal min, BigDecimal max, List<FieldOptionInput> options) {
        return new TypeFieldInput(name, type, null, null, secret, min, max, options);
    }

    @Test
    void unknownFieldType_isRejected() {
        assertTrue(messages(field("F", "COLOUR", false, null, null, List.of()))
                .contains("annotation.field.type.unknown"));
    }

    @Test
    void optionsOnNonChoiceField_isRejected() {
        assertTrue(messages(field("F", "TEXT", false, null, null, List.of(new FieldOptionInput("a", null))))
                .contains("annotation.field.options.not_allowed"));
    }

    @Test
    void badgeColourOutsidePalette_isRejected() {
        assertTrue(messages(field("Status", "LIST", false, null, null, List.of(new FieldOptionInput("open", "PURPLE"))))
                .contains("annotation.field.option.colour.invalid"));
    }

    @Test
    void badgeColourOnNonListField_isRejected() {
        assertTrue(messages(field("Env", "SINGLE_CHOICE", false, null, null, List.of(new FieldOptionInput("dev", "GREEN"))))
                .contains("annotation.field.option.colour.invalid"));
    }

    @Test
    void numberMinGreaterThanMax_isRejected() {
        assertTrue(messages(field("Port", "NUMBER", false, new BigDecimal("10"), new BigDecimal("5"), List.of()))
                .contains("annotation.field.number.bounds.invalid"));
    }

    @Test
    void secretOnNonTextField_isRejected() {
        assertTrue(messages(field("N", "NUMBER", true, null, null, List.of()))
                .contains("annotation.field.secret.not_allowed"));
    }

    @Test
    void blankFieldName_isRejected() {
        assertTrue(messages(field("", "TEXT", false, null, null, List.of()))
                .contains("annotation.field.name.required"));
    }

    @Test
    void validSecretTextField_hasNoViolations() {
        assertTrue(messages(field("API key", "TEXT", true, null, null, List.of())).isEmpty());
    }

    @Test
    void validListWithPaletteColours_hasNoViolations() {
        List<FieldOptionInput> options = List.of(new FieldOptionInput("open", "GREEN"), new FieldOptionInput("done", "BLUE"));
        assertTrue(messages(field("Status", "LIST", false, null, null, options)).isEmpty());
    }
}
