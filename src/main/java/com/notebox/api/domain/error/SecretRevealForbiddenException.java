package com.notebox.api.domain.error;

/** A caller without the elevated reveal role (Tenant administrator) attempted to reveal a secret value (BR-10, C-03). */
public class SecretRevealForbiddenException extends DomainException {

    public SecretRevealForbiddenException() {
        super(ErrorCategory.FORBIDDEN, "annotation.record.secret.reveal.forbidden");
    }
}
