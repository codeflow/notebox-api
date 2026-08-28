package com.notebox.api.domain.error;

/**
 * The email address is already registered. Which tenant holds it is deliberately not disclosed —
 * the message says only that the address is taken (BR-01).
 */
public class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException() {
        super(ErrorCategory.CONFLICT, "member.email.duplicate");
    }
}
