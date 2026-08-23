package com.notebox.api.domain.error;

/** The {@code group} listing filter was neither a group id nor the literal {@code "none"}. */
public class GroupFilterInvalidException extends DomainException {

    public GroupFilterInvalidException() {
        super(ErrorCategory.INVALID, "group.filter.invalid");
    }
}
