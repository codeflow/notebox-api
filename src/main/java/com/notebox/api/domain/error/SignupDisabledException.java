package com.notebox.api.domain.error;

/** This deployment does not accept new organizations. */
public class SignupDisabledException extends DomainException {

    public SignupDisabledException() {
        super(ErrorCategory.FORBIDDEN, "organization.signup.disabled");
    }
}
