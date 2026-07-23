package com.notebox.api.api.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Options may be declared only on List / Single choice / Multiple choice fields (FR-03). */
@Documented
@Constraint(validatedBy = OptionsAllowedValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface OptionsAllowedForFieldType {

    String message() default "annotation.field.options.not_allowed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
