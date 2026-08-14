package com.notebox.api.api.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * The value must be one of the {@link com.notebox.api.domain.Priority} enum names (FR-10, D7).
 * Passes null — absence is {@code @NotNull}'s concern (task.priority.required).
 */
@Constraint(validatedBy = PriorityValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPriority {

    String message() default "task.priority.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
