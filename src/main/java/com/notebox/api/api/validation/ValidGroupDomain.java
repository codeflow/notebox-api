package com.notebox.api.api.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * The value must be one of the {@link com.notebox.api.domain.GroupDomain} enum names (FR-08,
 * OQ-04). Passes null — absence is {@code @NotNull}'s concern (group.domain.required).
 */
@Constraint(validatedBy = GroupDomainValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGroupDomain {

    String message() default "group.domain.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
