package com.notebox.api.api.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** The Secret flag may be set only on Text and Free text fields (human decision 2026-07-23). */
@Documented
@Constraint(validatedBy = SecretAllowedValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface SecretAllowedForFieldType {

    String message() default "annotation.field.secret.not_allowed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
