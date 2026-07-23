package com.notebox.api.api.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** When both are present, a Number field's minimum must not exceed its maximum. */
@Documented
@Constraint(validatedBy = NumberBoundsValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface NumberBoundsValid {

    String message() default "annotation.field.number.bounds.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
