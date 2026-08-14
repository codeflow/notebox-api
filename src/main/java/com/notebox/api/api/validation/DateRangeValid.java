package com.notebox.api.api.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * A subtask's start date must not be after its end date when both are present (FR-11, OQ-09
 * refinement); either date alone — or neither — is valid.
 */
@Constraint(validatedBy = DateRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DateRangeValid {

    String message() default "task.subtask.date.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
