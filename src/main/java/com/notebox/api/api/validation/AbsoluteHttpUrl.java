package com.notebox.api.api.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * The value must be an absolute URL with scheme {@code http} or {@code https} and an authority
 * (FR-13 card link) — {@code javascript:}, {@code data:}, relative and authority-less forms such
 * as {@code https:foo} are unstorable, since the web renders the link as an anchor. Passes null —
 * absence is the caller's concern.
 */
@Constraint(validatedBy = AbsoluteHttpUrlValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface AbsoluteHttpUrl {

    String message() default "task.card.url.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
