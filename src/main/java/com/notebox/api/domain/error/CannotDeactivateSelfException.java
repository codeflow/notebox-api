package com.notebox.api.domain.error;

/** An administrator may not deactivate their own account — that is an accident, not an act. */
public class CannotDeactivateSelfException extends DomainException {

    public CannotDeactivateSelfException() {
        super(ErrorCategory.INVALID, "member.deactivate.self");
    }
}
