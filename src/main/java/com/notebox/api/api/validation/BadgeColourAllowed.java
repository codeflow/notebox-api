package com.notebox.api.api.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** An option badge colour must be in the palette and only on a List field (FR-03). */
@Documented
@Constraint(validatedBy = BadgeColourValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface BadgeColourAllowed {

    String message() default "annotation.field.option.colour.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
