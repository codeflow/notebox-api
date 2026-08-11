package com.notebox.api.api.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** A record request may carry at most one value per field id (BR-03, audit F1). */
@Documented
@Constraint(validatedBy = AtMostOneValuePerFieldValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface AtMostOneValuePerField {

    String message() default "annotation.record.value.duplicate_field";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
